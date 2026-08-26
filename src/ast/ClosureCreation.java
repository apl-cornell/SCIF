package ast;

import compile.CompileEnv;
import compile.ast.Statement;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import typecheck.ClosureTypeSym;
import typecheck.Context;
import typecheck.ExpOutcome;
import typecheck.FuncSym;
import typecheck.InterfaceSym;
import typecheck.Label;
import typecheck.NTCEnv;
import typecheck.PathOutcome;
import typecheck.PsiUnit;
import typecheck.ClosureTypeSym.RealLabels;
import typecheck.CodeLocation;
import typecheck.ScopeContext;
import typecheck.Sym;
import typecheck.TypeSym;
import typecheck.Utils;
import typecheck.VarSym;
import typecheck.VisitEnv;
import typecheck.sherrlocUtils.Constraint;
import typecheck.sherrlocUtils.Inequality;
import typecheck.sherrlocUtils.Relation;
import typecheck.exceptions.SemanticException;

/**
 * Closure creation: `closure callee(arg | _ , ...)`.
 *
 * `callee` is either `target.m` (a closurable method) or a closure-typed
 * value to partially bind. `args` mixes ordinary expressions and
 * {@link Hole} placeholders for still-unbound positions. Partial binding
 * over an existing closure value and codegen are not implemented yet.
 */
public class ClosureCreation extends Expression {

    public final Expression callee;
    public final List<Expression> args;

    // Store the IFC-type-checked labels (in callee's names)
    private RealLabels realLabels;
    private boolean ifcVisited = false;

    // Store type-checked funcSym
    FuncSym funcSym = null;
    /** For the bind form: the closure type being narrowed. */
    private ClosureTypeSym srcClosureSym;
    private String targetInterfaceName = null;
    private boolean ntced = false;

    RealLabels realLabels() {
        assert ifcVisited : "closure creation labels read before IFC visit: " + location;
        return realLabels;
    }

    /**
     * Shared logic for flow sites (assignment, call argument, return) whose receiving slot is closure-typed.
     */
    static void checkFlowInto(Expression source, ClosureTypeSym targetType,
            Map<String, String> targetMap, VisitEnv env,
            CodeLocation location, String description) throws SemanticException {
        targetType.registerAtoms(env);
        RealLabels src = null;
        if (source instanceof Name n) {
            VarSym var = env.getVar(n.id);
            if (var != null && var.typeSym instanceof ClosureTypeSym cs) {
                cs.registerAtoms(env);
                src = cs.renderLabels(new HashMap<>());
            }
        } else if (source instanceof ClosureCreation cc) {
            src = cc.realLabels();
        } else if (source instanceof Endorse e) {
            src = e.realLabels();
        } else if (source instanceof Call c) {
            src = c.realLabels();
        }

        if (src == null) {
            throw new SemanticException(
                    "cannot determine the closure labels of this expression; bind it to a closure-typed variable first", location);
        }
        src.emitSubtypeConstraints(targetType, targetMap, ineq ->
                env.cons.add(new Constraint(ineq, env.hypothesis(), location,
                        env.curContractSym().getName(), description)));
    }

    public ClosureCreation(Expression callee, List<Expression> args) {
        this.callee = callee;
        this.args = args == null ? new ArrayList<>() : args;
    }

    @Override
    public ScopeContext genTypeConstraints(NTCEnv env, ScopeContext parent)
            throws SemanticException {
        ScopeContext now = new ScopeContext(this, parent);
        List<TypeSym> declaredParams = new ArrayList<>();
        TypeSym retSym;

        if (callee instanceof Attribute att && att.value instanceof Name recv
                && env.getCurSym(recv.id) instanceof VarSym rv
                && rv.typeSym instanceof InterfaceSym contractSym) {
            // Creation. 

            Sym fsy = contractSym.getFunc(att.attr.id);
            if (!(fsy instanceof FuncSym fs)) {
                throw new SemanticException(
                        "closure target method not found: " + recv.id + "." + att.attr.id, location);
            }
            if (!fs.isClosurable()) {
                throw new SemanticException(
                        "method is not closurable: " + att.attr.id, location);
            }
            this.funcSym = fs;
            this.targetInterfaceName = contractSym.getName();
            this.ntced = true;
            for (VarSym p : fs.parameters) {
                declaredParams.add(p.typeSym);
            }
            retSym = fs.returnType;
        } else if (callee instanceof Name cn
                && env.getCurSym(cn.id) instanceof VarSym cv
                && cv.typeSym instanceof ClosureTypeSym cs) {
            // Bind. The closure being narrowed plays the role the
            // target's method type plays for creation.
            this.srcClosureSym = cs;
            this.ntced = true;
            declaredParams.addAll(cs.unboundParams);
            retSym = cs.returnType;
        } else {
            throw new SemanticException(
                    "closure head is neither a closurable method nor a closure value", location);
        }

        if (args.size() != declaredParams.size()) {
            throw new SemanticException(
                    "closure argument count does not match the target", location);
        }

        // Typecheck arguments
        List<TypeSym> stillUnbound = new ArrayList<>();
        for (int i = 0; i < args.size(); ++i) {
            Expression a = args.get(i);
            if (a instanceof Hole) {
                stillUnbound.add(declaredParams.get(i));
                continue;
            }
            ScopeContext ac = a.genTypeConstraints(env, now);
            env.addCons(ac.genTypeConstraints(
                    declaredParams.get(i).toSHErrLocFmt(), Relation.GEQ, env, a.location));
        }

        // Closure type eval
        ClosureTypeSym result =
                new ClosureTypeSym(stillUnbound, retSym, new ArrayList<>(), scopeContext);
        env.addCons(now.genEqualCons(result, env, location, "Type error: Improper closure"));
        return now;
    }

    /**
     * Information-flow constraints for the bind form,
     * {@code closure c(a, _, b)}: partially applying a value that is
     * already a closure.
     *
     * <p>[Bind] is [Create] without the receiver. The target was fixed
     * at creation, so the closure's own type plays the role the method
     * type plays there, and there is no {@code this} to substitute.
     * Arguments are positional over the source's still-unbound slots.
     *
     * <p>Two things are recomputed for the result. Its labels refer to
     * open slots positionally, so a slot that stays open is renumbered
     * and a slot filled here is replaced by the principal that filled
     * it — one substitution map, built in argument order so that a
     * label may name a slot bound before it but not one bound after.
     * And the value label is re-tainted by the control label here and
     * by the source closure's own value label.
     */
    private ExpOutcome bindIFConstraints(VisitEnv env, PathOutcome psi,
            String ifNamePc, String ifNameRtn, Context beginContext)
            throws SemanticException {
        if (!(callee instanceof Name cn)) {
            throw new SemanticException(
                    "closure head is neither a closurable method nor a closure value",
                    location);
        }
        VarSym srcVar = env.getVar(cn.id);
        if (srcVar == null || !(srcVar.typeSym instanceof ClosureTypeSym cs)) {
            throw new SemanticException(
                    "closure bind requires a closure-typed value: " + cn.id, location);
        }
        cs.registerAtoms(env);

        ExpOutcome so = callee.genIFConstraints(env, false);
        psi.joinExe(so.psi);
        String ifSrcRtn = so.valueLabelName; // the source closure's value label

        Map<String, String> bindMapping = new HashMap<>();
        List<VarSym> binders = cs.binderSyms();

        // Slots that stay open are renumbered: their positional tokens
        // in the result count only the slots that remain.
        int newOrdinal = 0;
        for (int i = 0; i < args.size(); i++) {
            if (args.get(i) instanceof Hole) {
                VarSym binder = i < binders.size() ? binders.get(i) : null;
                if (binder != null) {
                    bindMapping.put(binder.toSHErrLocFmt(),
                            ClosureTypeSym.binderToken(newOrdinal));
                    env.addRigidAtom(ClosureTypeSym.binderToken(newOrdinal));
                }
                newOrdinal++;
            }
        }

        for (int i = 0; i < args.size(); i++) {
            Expression a = args.get(i);
            if (a instanceof Hole) {
                continue;
            }

            // A slot filled with a principal becomes that principal
            // everywhere the result's labels named the slot.
            Expression principalExp = a;
            if (a instanceof Call cast && cast.isCast(env)) {
                principalExp = cast.getArgAt(0);
            }
            VarSym binder = i < binders.size() ? binders.get(i) : null;
            if (binder != null && principalExp instanceof Name an) {
                VarSym valueSym = env.getVar(an.id);
                if (valueSym != null && valueSym.isPrincipalVar()) {
                    bindMapping.put(binder.toSHErrLocFmt(), valueSym.toSHErrLocFmt());
                }
            }

            ExpOutcome ao = a.genIFConstraints(env, false);
            psi.joinExe(ao.psi);
            env.inContext = Utils.genNewContextAndConstraints(env, false,
                    ao.psi.getNormalPath().c, beginContext.lambda, a.nextPcSHL(), a.location);
            String argLabelStr = RealLabels.render(cs.getLabelArg(i), bindMapping);

            if (env.curContractSym().invoker().toSHErrLocFmt().equals(argLabelStr)) {
                throw new SemanticException(
                        "cannot bind the " + Utils.ordNumString(i + 1)
                                + " argument: its parameter label depends on the invoker;"
                                + " leave it unbound and supply it at invoke",
                        a.location);
            }

            if (argLabelStr != null
                    && argLabelStr.contains(ClosureTypeSym.BINDER_TOKEN_PREFIX)) {
                throw new SemanticException(
                        "cannot bind the " + Utils.ordNumString(i + 1)
                                + " argument: its parameter label depends on a"
                                + " still-unbound parameter",
                        a.location);
            }

            // argLabel[i] => paramLabel[i]
            env.cons.add(new Constraint(
                    new Inequality(ao.valueLabelName, Relation.LEQ, argLabelStr),
                    env.hypothesis(), a.location, env.curContractSym().getName(),
                    "Input to the " + Utils.ordNumString(i + 1)
                            + " bound closure argument must be trusted enough"));

            // pc => paramLabel[i]
            env.cons.add(new Constraint(
                    new Inequality(ifNamePc, Relation.LEQ, argLabelStr),
                    env.hypothesis(), a.location, env.curContractSym().getName(),
                    "Current control flow must be trusted to bind the "
                            + Utils.ordNumString(i + 1) + " closure argument"));
        }

        // pc => closureValueLabel
        env.cons.add(new Constraint(
                new Inequality(ifNamePc, Relation.LEQ, ifNameRtn),
                env.hypothesis(), location, env.curContractSym().getName(),
                "Closure value integrity is bounded by the binding control flow"));

        // sourceClosureLabel => closureValueLabel
        env.cons.add(new Constraint(
                new Inequality(ifSrcRtn, Relation.LEQ, ifNameRtn),
                env.hypothesis(), location, env.curContractSym().getName(),
                "Closure value integrity is bounded by the closure it binds"));

        // The result's labels are the source's under the substitution
        // above; only the slots that stayed open keep a parameter label.
        RealLabels all = cs.renderLabels(bindMapping);
        List<String> remaining = new ArrayList<>();
        for (int i = 0; i < args.size(); ++i) {
            if (args.get(i) instanceof Hole && i < all.paramLabels.size()) {
                remaining.add(all.paramLabels.get(i));
            }
        }
        this.realLabels = new RealLabels(all.pcEx, all.pcIn, all.gamma, all.retLabel,
                remaining);
        this.ifcVisited = true;

        return new ExpOutcome(ifNameRtn, psi);
    }

    @Override
    public ExpOutcome genIFConstraints(VisitEnv env, boolean tail_position)
            throws SemanticException {
        Context beginContext = env.inContext;
        Context endContext = new Context(Utils.getLabelNamePc(toSHErrLocFmt()),
                Utils.getLabelNameLock(toSHErrLocFmt()));
        Map<String, String> dependentLabelMapping = new HashMap<>();
        String ifNamePc = Utils.getLabelNamePc(scopeContext.getSHErrLocName());

        // closure value label
        String ifNameRtn = scopeContext.getSHErrLocName() + ".closure" + location.toString(); 

        PathOutcome psi = new PathOutcome(new PsiUnit(endContext));

        if (!(callee instanceof Attribute att && att.value instanceof Name recvName)) {
            return bindIFConstraints(env, psi, ifNamePc, ifNameRtn, beginContext);
        }

        // Creation: closure recv.m(args)
        ExpOutcome vo = att.value.genIFConstraints(env, false);
        psi.joinExe(vo.psi);
        String ifContRtn = vo.valueLabelName; // target's value label
        VarSym recvVar = env.getVar(recvName.id);
        InterfaceSym contractSym = (InterfaceSym) recvVar.typeSym;
        FuncSym funcSym = contractSym.getFunc(att.attr.id);

        // substitute callee.sender = invoker, callee.any = any
        // callee.this = final ? recv : value lbl
        dependentLabelMapping.put(funcSym.sender().toSHErrLocFmt(),
                env.curContractSym().invoker().toSHErrLocFmt());
        dependentLabelMapping.put(contractSym.any().toSHErrLocFmt(),
                env.curContractSym().any().toSHErrLocFmt());
        if (recvVar.isFinal) {
            dependentLabelMapping.put(contractSym.thisSym().toSHErrLocFmt(),
                    recvVar.toSHErrLocFmt());
        } else {
            dependentLabelMapping.put(contractSym.thisSym().toSHErrLocFmt(), ifContRtn);
        }

        // A still-unbound principal parameter is referable in the
        // closure's labels only as its positional binder; map the
        // target's parameter to the canonical token so the baked labels
        // align with whatever binder name a declared closure type uses.
        int unboundOrdinal = 0;
        for (int i = 0; i < args.size(); i++) {
            if (args.get(i) instanceof Hole) {
                VarSym unboundParam = funcSym.parameters.get(i);
                if (unboundParam.isPrincipalVar()) {
                    dependentLabelMapping.put(unboundParam.toSHErrLocFmt(),
                            ClosureTypeSym.binderToken(unboundOrdinal));
                    env.addRigidAtom(ClosureTypeSym.binderToken(unboundOrdinal));
                }
                unboundOrdinal++;
            }
        }

        // Evaluate args
        for (int i = 0; i < args.size(); i++) {
            Expression a = args.get(i);
            if (a instanceof Hole) {
                // Unbound
                continue;
            }

            VarSym argSym = funcSym.parameters.get(i);

            Expression principalExp = a;
            if (a instanceof Call cast && cast.isCast(env)) {
                principalExp = cast.getArgAt(0);
            }

            if (argSym.isPrincipalVar() && principalExp instanceof Name an) {
                VarSym valueSym = env.getVar(an.id);
                if (valueSym != null && valueSym.isPrincipalVar()) {
                    dependentLabelMapping.put(argSym.toSHErrLocFmt(),
                            valueSym.toSHErrLocFmt());
                }
            }

            ExpOutcome ao = a.genIFConstraints(env, false);
            psi.joinExe(ao.psi);
            env.inContext = Utils.genNewContextAndConstraints(env, false,
                    ao.psi.getNormalPath().c, beginContext.lambda, a.nextPcSHL(), a.location);
            Label argLabel = funcSym.getLabelArg(i);
            String argLabelStr = argLabel.toSHErrLocFmt(dependentLabelMapping);

            if (env.curContractSym().invoker().toSHErrLocFmt().equals(argLabelStr)) {
                throw new SemanticException(
                        "cannot bind the " + Utils.ordNumString(i + 1)
                                + " argument at creation: its parameter label depends on the invoker;"
                                + " leave it unbound and supply it at invoke",
                        a.location);
            }

            // A slot whose label depends on a still-unbound principal
            // parameter cannot be bound yet — no principal exists to
            // check the bound value against until invoke supplies one.
            if (argLabelStr.contains(ClosureTypeSym.BINDER_TOKEN_PREFIX)) {
                throw new SemanticException(
                        "cannot bind the " + Utils.ordNumString(i + 1)
                                + " argument at creation: its parameter label depends on a"
                                + " still-unbound parameter",
                        a.location);
            }

            // argLabel[i] => paramLabel[i]
            env.cons.add(new Constraint(
                    new Inequality(ao.valueLabelName, Relation.LEQ, argLabelStr),
                    env.hypothesis(), a.location, env.curContractSym().getName(),
                    "Input to the " + Utils.ordNumString(i + 1)
                            + " bound closure argument must be trusted enough"));
            
            // pc => paramLabel[i]
            env.cons.add(new Constraint(
                    new Inequality(ifNamePc, Relation.LEQ, argLabelStr),
                    env.hypothesis(), a.location, env.curContractSym().getName(),
                    "Current control flow must be trusted to bind the "
                            + Utils.ordNumString(i + 1) + " closure argument"));
        }

        // pc => closureValueLabel
        env.cons.add(new Constraint(
                new Inequality(ifNamePc, Relation.LEQ, ifNameRtn),
                env.hypothesis(), location, env.curContractSym().getName(),
                "Closure value integrity is bounded by the creating control flow"));
        
        // recv..lbl => closureValueLabel
        env.cons.add(new Constraint(
                new Inequality(ifContRtn, Relation.LEQ, ifNameRtn),
                env.hypothesis(), location, env.curContractSym().getName(),
                "Closure value integrity is bounded by the target"));

        List<String> realParamLabels = new ArrayList<>();
        for (int i = 0; i < args.size(); ++i) {
            if (args.get(i) instanceof Hole) {
                realParamLabels.add(
                        RealLabels.render(funcSym.getLabelArg(i), dependentLabelMapping));
            }
        }
        this.realLabels = new RealLabels(
                RealLabels.render(funcSym.externalPc(), dependentLabelMapping),
                // pc_2 dropped
                // RealLabels.render(funcSym.internalPc(), dependentLabelMapping),
                null,
                RealLabels.render(funcSym.callGamma(), dependentLabelMapping),
                RealLabels.render(funcSym.endPc(), dependentLabelMapping),
                realParamLabels);
        this.ifcVisited = true;

        return new ExpOutcome(ifNameRtn, psi);
    }

    /**
     * Lower `closure recv.m(args)` to building a Closure value:
     * positional indices and ABI-encoded values of the bound arguments
     * go into two locals, then `makeClosure` packs them behind the
     * target's closure-entry selector (which the Solidity compiler
     * derives from the interface member reference).
     */
    @Override
    public compile.ast.Expression solidityCodeGen(List<Statement> result, CompileEnv code) {
        assert ntced : "closure creation compiled before regular typechecking: " + location;
        if (!(callee instanceof Attribute att && att.value instanceof Name)) {
            return bindCodeGen(result, code);
        }

        int arity = funcSym.parameters.size();
        List<Integer> boundIdx = new ArrayList<>();
        for (int i = 0; i < args.size(); ++i) {
            if (!(args.get(i) instanceof Hole)) {
                boundIdx.add(i);
            }
        }
        // dynMask describes the *target's signature*, not what this
        // creation happens to bind: both _bindSlots and invoke consult
        // it for every slot, including ones filled later.
        int dynMask = 0;
        for (int i = 0; i < arity; ++i) {
            compile.ast.Type paramT = funcSym.parameters.get(i).typeSym.getType();
            switch (compile.Utils.abiKind(paramT)) {
                case DYNAMIC -> dynMask |= (1 << i);
                case UNSUPPORTED -> throw new UnsupportedOperationException(
                        "closure arguments of this type not supported yet");
                default -> { }
            }
        }

        int id = code.nextClosureTempId();
        String ksName = "closureKs" + id;
        String valsName = "closureVals" + id;
        result.add(new compile.ast.VarDec(
                new compile.ast.ArrayType(new compile.ast.PrimitiveType(compile.Utils.PRIMITIVE_TYPE_UINT_NAME)), ksName,
                new compile.ast.Literal("new uint256[](" + boundIdx.size() + ")")));
        result.add(new compile.ast.VarDec(
                new compile.ast.ArrayType(new compile.ast.PrimitiveType("bytes")), valsName,
                new compile.ast.Literal("new bytes[](" + boundIdx.size() + ")")));
        for (int k = 0; k < boundIdx.size(); ++k) {
            int i = boundIdx.get(k);
            result.add(new compile.ast.Assign(
                    new compile.ast.Subscript(new compile.ast.SingleVar(ksName),
                            new compile.ast.Literal(String.valueOf(k))),
                    new compile.ast.Literal(String.valueOf(i))));
            compile.ast.Expression argExp = args.get(i).solidityCodeGen(result, code);
            result.add(new compile.ast.Assign(
                    new compile.ast.Subscript(new compile.ast.SingleVar(valsName),
                            new compile.ast.Literal(String.valueOf(k))),
                    new compile.ast.Call("abi.encode", List.of(argExp))));
        }

        compile.ast.Expression recvExp = att.value.solidityCodeGen(result, code);
        String closureEntryName = typecheck.Utils.closureMethodNameHash(
                funcSym.funcName, funcSym.plainSignature());
        code.markClosureStructRequired();
        code.markClosureLibRequired();
        return new compile.ast.Call("makeClosure", List.of(
                new compile.ast.Call("address", List.of(recvExp)),
                new compile.ast.Literal(
                        targetInterfaceName + "." + closureEntryName + ".selector"),
                new compile.ast.Literal(String.valueOf(arity)),
                new compile.ast.Literal(String.valueOf(dynMask)),
                new compile.ast.SingleVar(ksName),
                new compile.ast.SingleVar(valsName)));
    }

    /**
     * Lower {@code closure c(a, _, b)} to {@code bindClosure}: the same
     * index/value arrays creation builds, handed to the runtime with
     * the closure being narrowed. Nothing else is needed — the target,
     * the selector, the arity and the dynamic-slot mask were all fixed
     * at creation and travel inside the closure.
     *
     * <p>The indices are positions among the source's still-unbound
     * slots, which is exactly what the argument positions are and what
     * {@code _bindSlots} expects, so no translation happens here.
     */
    private compile.ast.Expression bindCodeGen(List<Statement> result, CompileEnv code) {
        ClosureTypeSym cs = srcClosureSym;
        List<Integer> boundIdx = new ArrayList<>();
        for (int i = 0; i < args.size(); ++i) {
            if (!(args.get(i) instanceof Hole)) {
                if (compile.Utils.abiKind(cs.unboundParams.get(i).getType())
                        == compile.Utils.AbiKind.UNSUPPORTED) {
                    throw new UnsupportedOperationException(
                            "closure arguments of this type not supported yet");
                }
                boundIdx.add(i);
            }
        }

        int id = code.nextClosureTempId();
        String ksName = "closureKs" + id;
        String valsName = "closureVals" + id;
        result.add(new compile.ast.VarDec(
                new compile.ast.ArrayType(new compile.ast.PrimitiveType(
                        compile.Utils.PRIMITIVE_TYPE_UINT_NAME)), ksName,
                new compile.ast.Literal("new uint256[](" + boundIdx.size() + ")")));
        result.add(new compile.ast.VarDec(
                new compile.ast.ArrayType(new compile.ast.PrimitiveType(
                        compile.Utils.PRIMITIVE_TYPE_BYTES_NAME)), valsName,
                new compile.ast.Literal("new bytes[](" + boundIdx.size() + ")")));
        for (int k = 0; k < boundIdx.size(); ++k) {
            int i = boundIdx.get(k);
            result.add(new compile.ast.Assign(
                    new compile.ast.Subscript(new compile.ast.SingleVar(ksName),
                            new compile.ast.Literal(String.valueOf(k))),
                    new compile.ast.Literal(String.valueOf(i))));
            compile.ast.Expression argExp = args.get(i).solidityCodeGen(result, code);
            result.add(new compile.ast.Assign(
                    new compile.ast.Subscript(new compile.ast.SingleVar(valsName),
                            new compile.ast.Literal(String.valueOf(k))),
                    new compile.ast.Call("abi.encode", List.of(argExp))));
        }

        compile.ast.Expression srcExp = callee.solidityCodeGen(result, code);
        code.markClosureStructRequired();
        code.markClosureLibRequired();
        return new compile.ast.Call(compile.Utils.RUNTIME_FUNC_BIND_CLOSURE,
                List.of(srcExp, new compile.ast.SingleVar(ksName),
                        new compile.ast.SingleVar(valsName)));
    }

    @Override
    public boolean typeMatch(Expression expression) {
        return expression instanceof ClosureCreation;
    }

    @Override
    public List<Node> children() {
        List<Node> rtn = new ArrayList<>();
        if (callee != null) rtn.add(callee);
        rtn.addAll(args);
        return rtn;
    }
}

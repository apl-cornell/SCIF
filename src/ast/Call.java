package ast;

import compile.CompileEnv;
import compile.ast.Assign;
import compile.ast.Attr;
import compile.ast.BinaryExpression;
import compile.ast.ExternalCall;
import compile.ast.IfStatement;
import compile.ast.Literal;
import compile.ast.Pass;
import compile.ast.PrimitiveType;
import compile.ast.Return;
import compile.ast.SingleVar;
import compile.ast.Statement;
import compile.ast.Type;
import compile.ast.VarDec;

import java.util.List;
import java.util.Map.Entry;
import java.util.stream.Collectors;

import typecheck.exceptions.SemanticException;
import typecheck.sherrlocUtils.Constraint;
import typecheck.sherrlocUtils.Inequality;
import typecheck.sherrlocUtils.Relation;
import typecheck.*;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

public class Call extends TrailerExpr {

    List<Expression> args;


    // store the called func symbol after regular typechecking
    FuncSym funcSym = null;
    private boolean isCast;
    boolean builtIn = false, ntced = false;
    CallSpec callSpec;

    // Store the IFC-type-checked labels (in callee's names), null if not applicable
    private ClosureTypeSym.RealLabels realLabels;

    ClosureTypeSym.RealLabels realLabels() {
        return realLabels;
    }

    private boolean isClosureInvoke = false;
    // Closure's external begin label (lextbef)
    private List<String> closurePcExLeaves = null;
    /** The invoked closure's declared type, kept for codegen. */
    private ClosureTypeSym closureSym = null;

    public Call() {
        this.args = new ArrayList<>();
    }

    public Call(Call call) {
        value = call.value;
        args = call.args;
        if (value instanceof Name) {
            value = new Name(((Name) value).id);
        }
    }

    public Call(Expression x, List<Expression> ys) {
        value = x;
        args = ys;
    }

    public void addArg(Expression arg) {
        this.args.add(arg);
    }

    private void setArgs(List<Expression> args) {
        this.args = args;
    }

    public void setSpec(CallSpec callSpec) {
        this.callSpec = callSpec;
    }

    public Expression getArgAt(int index) {
        return args.get(index);
    }

    /**
     * Resolve the callee (internal call, external method, or a built-in on
     * an array/closure value) and emit type constraints relating each
     * argument to its parameter and this call to the return type.
     */
    public ScopeContext genTypeConstraints(NTCEnv env, ScopeContext parent) throws SemanticException {
        this.ntced = true;
        ScopeContext now = new ScopeContext(this, parent);
        String funcName;
        FuncSym funcSym;
        boolean extern = false;
        if (!(value instanceof Name)) {
            if (!(value instanceof Attribute att)) {
                throw new SemanticException("type error in call (callee must be a simple name or an attribute): ", this.location);
            }
            // a.b(c), a must be a contract or an array

            extern = true;
            assert att.value instanceof Name : "at " + location.errString();
            String varName = ((Name) att.value).id; // a
            // System.out.println(varName);
            funcName = att.attr.id; // b
            Sym s = env.getCurSym(varName);
            assert s != null: "variable not found: " + varName + " at " + location.errString();
            // logger.debug("var " + varName + ": " + s.getName());

            if (!(s instanceof VarSym varSym)) {
                throw new SemanticException("type error in call (callee must be a variable): ", this.location);
            }

            if (varSym.typeSym instanceof InterfaceSym contractSym) {
                s = contractSym.getFunc(funcName);
                if (s == null)
                    throw new SemanticException("function " + varName + "." + funcName + "() not found",
                        location);
                funcSym = (FuncSym) s;

            } else if (varSym.typeSym instanceof ArrayTypeSym arrayTypeSym) {
                // TODO: change the hard-code style
                // TODO: factor this block out into its own method
                TypeSym arrayTSym = arrayTypeSym.valueType;
                String arrayTName = arrayTSym.toSHErrLocFmt();
                this.builtIn = true;
                if (funcName.equals("pop")) {
                    // return T
                    if (!args.isEmpty()) throw new SemanticException("pop expects 0 arguments",
                            this.location);
                    env.addCons(now.genTypeConstraints(arrayTName, Relation.EQ, env, location));
                    return now;
                } else if (funcName.equals("push")) {
                    // require one T, return void
                    if (args.size() != 1) throw new SemanticException("push expects 1 argument",
                            this.location);
                    Expression arg = args.get(0);
                    ScopeContext argContext = arg.genTypeConstraints(env, now);
                    env.addCons(argContext.genTypeConstraints(arrayTName, Relation.GEQ, env, arg.location));
                    TypeSym rtnTypeSym = (TypeSym) env.getSym(BuiltInT.VOID);
                    env.addCons(now.genTypeConstraints(rtnTypeSym.toSHErrLocFmt(), Relation.EQ, env, location));
                    return now;
                } else if (funcName.equals("length")) {
                    // return uint
                    if (!args.isEmpty()) {
                        throw new SemanticException("length expects 0 arguments",
                                this.location);
                    }
                    TypeSym rtnTypeSym = (TypeSym) env.getSym(BuiltInT.UINT);
                    env.addCons(now.genTypeConstraints(rtnTypeSym.toSHErrLocFmt(), Relation.EQ, env, location));
                    return now;
                } else {
                    throw new SemanticException("type error: unknown array operator", this.location);
                }
            } else if (varSym.typeSym instanceof ClosureTypeSym closureTypeSym) {
                // c.invoke(args): args must match the still-unbound parameters; the call's type is the closure's return.
                this.builtIn = true;
                if (!funcName.equals("invoke")) {
                    throw new SemanticException("type error: unknown closure operator " + funcName, this.location);
                }
                if (args.size() != closureTypeSym.unboundParams.size()) {
                    throw new SemanticException(
                            "invoke argument count does not match the closure", this.location);
                }
                for (int i = 0; i < args.size(); ++i) {
                    Expression arg = args.get(i);
                    ScopeContext argContext = arg.genTypeConstraints(env, now);
                    env.addCons(argContext.genTypeConstraints(
                            closureTypeSym.unboundParams.get(i).toSHErrLocFmt(),
                            Relation.GEQ, env, arg.location));
                }
                env.addCons(now.genTypeConstraints(
                        closureTypeSym.returnType.toSHErrLocFmt(), Relation.EQ, env, location));
                return now;
            } else {
                throw new SemanticException("type error: " + varName + "." + funcName + "() " + varSym.typeSym.toSHErrLocFmt() + (varSym.typeSym instanceof ContractSym),
                    this.location);
            }
        } else {
            // a(b) - internal contract call
            funcName = ((Name) value).id;
            // System.out.println(funcName);
            Sym s;
            if (funcName.equals(Utils.SUPER_KEYWORD)) {
                if (env.superCalled())
                    throw new SemanticException("cannot call super() twice in the constructor: ", location);
                if (!env.inConstructor())
                    throw new SemanticException("cannot call super() outside the constructor: " , location);
                String newFuncName = Utils.genSuperName(env.curContractSym().getName());
                s = env.getCurSym(newFuncName);
                if (s == null) {
                    throw new SemanticException("could not find superclass constructor",
                            location);
                }
                // TODO: think twice
                 ((Name) value).id = newFuncName;
                env.callSuper();
            } else {
                s = env.getCurSym(funcName);
            }
            if (s == null) {
                throw new SemanticException("method not found: " + funcName,
                    location);
            }
            if (!(s instanceof FuncSym)) {
                if (s instanceof InterfaceSym || s instanceof BuiltinTypeSym) {
                    env.addCons(now.genTypeConstraints(s.getName(), Relation.EQ, env, location));
                    isCast = true;
                    return now;
                }
                throw new SemanticException("contract not found: " + s.getName(),
                    location);
            }
            funcSym = ((FuncSym) s);
            if (funcSym.isBuiltIn()) {
                this.builtIn = true;
            }
        }
        if (env.inConstructor() && !env.superCalled())
            throw new SemanticException("cannot call methods before super called in constructor: " + funcName,
                 location);
        if (args.size() != funcSym.parameters.size())
            throw new SemanticException("number of arguments does not match the number of parameters of the called method: " + funcName,
                location);
        this.funcSym = funcSym;

        if (extern && callSpec != null) {
            callSpec.genTypeConstraints(env, now);
        }
        // typecheck arguments
        for (int i = 0; i < args.size(); ++i) {
            Expression arg = args.get(i);
            TypeSym paraInfo = funcSym.parameters.get(i).typeSym;
            ScopeContext argContext = arg.genTypeConstraints(env, now);
            String typeName = paraInfo.toSHErrLocFmt();
            env.addCons(argContext.genTypeConstraints(typeName, Relation.GEQ, env, arg.location));
        }
        String rtnTypeName = funcSym.returnType.toSHErrLocFmt();
        env.addCons(now.genTypeConstraints(rtnTypeName, Relation.EQ, env, location));

        for (Map.Entry<ExceptionTypeSym, String> tl : funcSym.exceptions.entrySet()) {
            if (!parent.isCheckedException(tl.getKey(), extern)) {
                throw new SemanticException("Unchecked exception: " + tl.getKey().getName(),
                    location);
            }
        }
        return now;
    }

    /**
     * Emit the information-flow constraints for this call expression.
     *
     * Dispatches on the callee's shape: array built-ins ({@code pop} /
     * {@code push} / {@code length}), closure invocation
     * ({@code c.invoke(...)}), an external method call on another
     * contract, a local method call, or a type cast. 
     *
     * @param env the visit environment: symbol tables, the current
     *         (pc, lock) context in {@code env.inContext}, the trust
     *         hypothesis, and the sink ({@code env.cons}) that collects
     *         emitted constraints
     * @param tail_position 
     * @return the outcome of this expression: the SHErrLoc label naming
     *         the result value's integrity, and the per-path (pc, lock)
     *         outcomes
     * @throws SemanticException if the callee cannot be resolved, a cast
     *         is malformed, or a closure value is used with an
     *         operator other than {@code invoke}
     */
    @Override
    public ExpOutcome genIFConstraints(VisitEnv env, boolean tail_position)
            throws SemanticException {
        //TODO: Assuming value is a Name for now
        Context beginContext = env.inContext; // (pc, lambda)
        Context endContext = new Context(Utils.getLabelNamePc(toSHErrLocFmt()),
                Utils.getLabelNameLock(toSHErrLocFmt()));
        Map<String, String> dependentLabelMapping = new HashMap<>();

        List<String> argValueLabelNames = new ArrayList<>();

        PathOutcome psi = new PathOutcome(new PsiUnit(endContext));

        // Evaluate arguments from left to right with substituations.
        ExpOutcome ao = null;
        for (Expression arg : args) {
            ao = arg.genIFConstraints(env, false);
            psi.joinExe(ao.psi);
            argValueLabelNames.add(ao.valueLabelName);

            env.inContext = Utils.genNewContextAndConstraints(env, false,
                    ao.psi.getNormalPath().c, beginContext.lambda, arg.nextPcSHL(), arg.location);
//            env.inContext = new Context(
//                    Utils.joinLabels(ao.psi.getNormalPath().c.pc, beginContext.pc),
//                    beginContext.lambda);
        }

        String funcName;
        String ifNamePc; // currentMethod.PC
        FuncSym funcSym;
        String namespace = "";
        Label ifFuncCallPcBefore, ifFuncCallPcAfter, ifFuncGammaLock;

        ExpOutcome vo = null;

        boolean externalCall = false;
        InterfaceSym externalContractSym = null;
        VarSym externalTargetSym = null;
        String ifContRtn = null;

        if (!(value instanceof Name)) {
            if (!(value instanceof Attribute att)) {
                throw new Error("Internal compiler error" + location.errString());
            }

            // case 1: a.b(c) where a is a contract or an array, b is a function and c are the arguments
            // att = a.b

            externalCall = true;

            // evaluate a.b
            vo = att.value.genIFConstraints(env, false);
            psi.joinExe(vo.psi);
            ifContRtn = vo.valueLabelName; // a..lbl

            //TODO: assuming a's depth is 1
            String varName = ((Name) att.value).id; // a
            funcName = (att.attr).id; // b
            VarSym var = env.getVar(varName);

            if (var.typeSym instanceof ArrayTypeSym arrayTypeSym) {
                // case 1a: a is an array (terminating branch)

                TypeSym arrayTSym = arrayTypeSym.valueType;
                String arrayTName = arrayTSym.toSHErrLocFmt();

                //TODO: change the hard-code style
                if (funcName.equals("pop")) {
                    // requires pc => integrity of the array var
                    Utils.contextFlow(env, psi.getNormalPath().c, endContext, location);
                    ifNamePc = Utils.getLabelNamePc(scopeContext.getSHErrLocName());
                    env.cons.add(
                            new Constraint(new Inequality(ifNamePc, ifContRtn), env.hypothesis(),
                                    location,
                                    "Current control flow must be trusted to call this method"));
                    // pc => l
                    if (!tail_position) {
                        env.cons.add(new Constraint(
                                new Inequality(psi.getNormalPath().c.lambda, beginContext.lambda),
                                env.hypothesis(), location,
                                Utils.ERROR_MESSAGE_LOCK_IN_NONLAST_OPERATION));
                    }
                    return new ExpOutcome(ifNamePc, psi);
                } else if (funcName.equals("push")) {
                    // require pc => integrity of the array var
                    // require the element => integrity of the array var
                    Expression arg = args.get(0);
                    ExpOutcome argOutcome = arg.genIFConstraints(env, false);
                    psi.join(argOutcome.psi);
                    String argLabel = argOutcome.valueLabelName;
                    Utils.contextFlow(env, psi.getNormalPath().c, endContext, location);
                    ifNamePc = Utils.getLabelNamePc(scopeContext.getSHErrLocName());
                    env.cons.add(
                            new Constraint(new Inequality(ifNamePc, var.ifl.toSHErrLocFmt()), env.hypothesis(),
                                    location, env.curContractSym().getName(),
                                    "Current control flow must be trusted to call this method"));
                    // pc => ?
                    env.cons.add(
                            new Constraint(new Inequality(argLabel, var.ifl.toSHErrLocFmt()), env.hypothesis(),
                                    location, env.curContractSym().getName(),
                                    "Current control flow must be trusted to call this method"));
                    if (!tail_position) {
                        env.cons.add(new Constraint(
                                new Inequality(psi.getNormalPath().c.lambda, beginContext.lambda),
                                env.hypothesis(), location, env.curContractSym().getName(),
                                Utils.ERROR_MESSAGE_LOCK_IN_NONLAST_OPERATION));
                    }
                    return new ExpOutcome(ifNamePc, psi);
                } else if (funcName.equals("length")) {
                    // return uint the same as
                    Utils.contextFlow(env, psi.getNormalPath().c, endContext, location);
                    ifNamePc = Utils.getLabelNamePc(scopeContext.getSHErrLocName());
                    if (!tail_position) {
                        env.cons.add(new Constraint(
                                new Inequality(psi.getNormalPath().c.lambda, beginContext.lambda),
                                env.hypothesis(), location, env.curContractSym().getName(),
                                Utils.ERROR_MESSAGE_LOCK_IN_NONLAST_OPERATION));
                    }
                    return new ExpOutcome(var.ifl.toSHErrLocFmt(), psi);
                } else {
                    throw new SemanticException("Unrecognized operator", location);
                }
            } 
            
            if (var.typeSym instanceof ClosureTypeSym cs) {
                // case 1b: a is a closure value; a.invoke(args) (terminating branch)

                if (!funcName.equals("invoke")) {
                    throw new SemanticException(
                            "type error: unknown closure operator " + funcName, location);
                }

                this.builtIn = true;
                this.isClosureInvoke = true;
                this.closureSym = cs;
                cs.registerAtoms(env);

                // lextbef
                this.closurePcExLeaves = cs.pcEx().principalLeaves();
                ifNamePc = Utils.getLabelNamePc(scopeContext.getSHErrLocName());
                dependentLabelMapping.put(env.curContractSym().invoker().toSHErrLocFmt(), env.thisSym().toSHErrLocFmt());

                // Each invoke argument naming a principal fills its
                // positional binder in the closure's labels. An argument
                // for a label-referenced slot must name a principal (the
                // dependent-map key rule).
                List<VarSym> binders = cs.binderSyms();
                for (int i = 0; i < args.size() && i < binders.size(); i++) {
                    VarSym binder = binders.get(i);
                    if (binder == null) {
                        continue;
                    }
                    Expression principalExp = args.get(i);
                    if (principalExp instanceof Call cast && cast.isCast(env)) {
                        principalExp = cast.getArgAt(0);
                    }
                    VarSym actual = principalExp instanceof Name an ? env.getVar(an.id) : null;
                    if (actual != null && actual.isPrincipalVar()) {
                        dependentLabelMapping.put(binder.toSHErrLocFmt(),
                                actual.toSHErrLocFmt());
                    } else if (cs.referencesBinder(binder)) {
                        throw new SemanticException(
                                "must use a final address/contract for the "
                                        + Utils.ordNumString(i + 1)
                                        + " invoke argument: the closure's labels depend on it",
                                args.get(i).location);
                    }
                }

                String pcExStr = cs.pcEx().toSHErrLocFmt(dependentLabelMapping);
                // pc_2 dropped
                // String pcInStr  = cs.pcIn().toSHErrLocFmt(dependentLabelMapping);
                String gammaStr = cs.callGamma().toSHErrLocFmt(dependentLabelMapping);
                String retStr = cs.endPc().toSHErrLocFmt(dependentLabelMapping);

                for (int i = 0; i < args.size(); i++) {
                    Expression arg = args.get(i);
                    String argValue = argValueLabelNames.get(i);
                    String argLabelStr = cs.getLabelArg(i).toSHErrLocFmt(dependentLabelMapping);
                    // argValue => paramLabel[i]
                    env.cons.add(new Constraint(
                            new Inequality(argValue, Relation.LEQ, argLabelStr),
                            env.hypothesis(), arg.location, env.curContractSym().getName(),
                            "Input to the " + Utils.ordNumString(i + 1)
                                    + " invoke argument must be trusted enough"));
                    
                    // pc => paramLabel[i]
                    env.cons.add(new Constraint(
                            new Inequality(ifNamePc, Relation.LEQ, argLabelStr),
                            env.hypothesis(), arg.location, env.curContractSym().getName(),
                            "Current control flow must be trusted to feed the "
                                    + Utils.ordNumString(i + 1) + "-th invoke argument"));
                }

                // Authorization to make the call

                // closureValueLabel => pc_ex
                env.cons.add(new Constraint(
                        new Inequality(ifContRtn, pcExStr),
                        env.hypothesis(), location, env.curContractSym().getName(),
                        "Closure value must be trusted enough to invoke"));

                // pc => pc_ex
                env.cons.add(new Constraint(
                        new Inequality(ifNamePc, pcExStr),
                        env.hypothesis(), location, env.curContractSym().getName(),
                        "Current control flow must be trusted to invoke this closure"));

                // pc_2 dropped
                // // pc_ex => pc_in ∨ lambda_caller
                // env.cons.add(new Constraint(
                //         new Inequality(pcExStr,
                //                 Utils.joinLabels(pcInStr, beginContext.lambda)),
                //         env.hypothesis(), location, env.curContractSym().getName(),
                //         "Invoking this closure does not respect static reentrancy locks"));

                // Post-call Psi — normal path: (pc ⊔ l_nθ_u ⊔ ℓ, gamma).
                // B.8 / advisor item (3): the continuation is floored by the
                // caller's pc, the believed return label, and the closure's
                // value label. pc_ex is an entry requirement (gate premise +
                // runtime witness), not a taint source; joining it here would
                // nullify callee-declared autoendorsement (any-entry methods
                // would taint every caller's continuation to any).
                String preCallPc = psi.getNormalPath().c.pc;
                String postPcJoin = Utils.joinLabels(
                        Utils.joinLabels(preCallPc, retStr),
                        ifContRtn);
                PathOutcome expPsi = new PathOutcome(new PsiUnit(new Context(
                        postPcJoin, gammaStr)));

                // TODO: 
                // closureValueLabel ⊔ gamma == post-call lambda
                env.cons.add(new Constraint(
                        new Inequality(Utils.joinLabels(ifContRtn, gammaStr),
                                Relation.EQ, endContext.lambda),
                        env.hypothesis(), location, env.curContractSym().getName(),
                        "Invoking this closure does not respect static reentrancy locks"));

                if (!tail_position) {
                    env.cons.add(new Constraint(
                            new Inequality(psi.getNormalPath().c.lambda, beginContext.lambda),
                            env.hypothesis(), location, env.curContractSym().getName(),
                            Utils.ERROR_MESSAGE_LOCK_IN_NONLAST_OPERATION)
                        ); // normal psi.lock => pre-call lamnbda
                }

                // Finalize
                String ifNameFuncRtnValue = retStr;
                psi.joinExe(expPsi);
                // contextFlow would route the pc edge into trustCons, which
                // createDiagnoser hands to SHErrLoc as ASSUMPTIONS — granting
                // the flow instead of checking it (found 2026-07-19; the Ψ
                // floor was unenforced for every call). Emit the lock edge on
                // the usual assumption channel but the pc floor as a CHECKED
                // constraint.
                env.addTrustConstraint(new Constraint(
                        new Inequality(psi.getNormalPath().c.lambda, endContext.lambda),
                        env.hypothesis(), location, env.curContractSym().getName(),
                        "actually maintained lock of final sub-statement must flow to that of parent statement"));
                // Emitted component-wise: a join on the left of ≤ can slip
                // through the solver unchecked (see IMPLEMENTATION.md,
                // string-emission landmines).
                env.cons.add(new Constraint(
                        new Inequality(preCallPc, endContext.pc),
                        env.hypothesis(), location, env.curContractSym().getName(),
                        "Post-invocation control flow must carry the pre-call control flow"));
                env.cons.add(new Constraint(
                        new Inequality(retStr, endContext.pc),
                        env.hypothesis(), location, env.curContractSym().getName(),
                        "Post-invocation control flow must carry the closure's return label"));
                env.cons.add(new Constraint(
                        new Inequality(ifContRtn, endContext.pc),
                        env.hypothesis(), location, env.curContractSym().getName(),
                        "Post-invocation control flow must carry the closure's value label"));
                psi.setNormalPath(endContext);
                env.cons.add(new Constraint(
                        new Inequality(psi.getNormalPath().c.pc, ifNameFuncRtnValue),
                        env.hypothesis(), location, env.curContractSym().getName(), "ln_to_t"));
                return new ExpOutcome(ifNameFuncRtnValue, psi);
            }

            // case 1c: external call
            // look up funcSym, update dependentLabelMapping, and read labels from funcSym

            externalTargetSym = var;
            namespace = var.toSHErrLocFmt();
            TypeSym conType = var.typeSym;
            externalContractSym = env.getContract(conType.getName());

            env.addSigReq(namespace, conType.getName());
            ifNamePc = Utils.getLabelNamePc(scopeContext.getSHErrLocName());
            InterfaceSym contractSym = env.getContract(conType.getName());
            funcSym = contractSym.getFunc(funcName);
            if (funcSym == null)
                throw new SemanticException("not found: " + conType.getName() + "." + funcName, location);

            dependentLabelMapping.put(funcSym.sender().toSHErrLocFmt(), env.thisSym().toSHErrLocFmt());
            dependentLabelMapping.put(contractSym.any().toSHErrLocFmt(), env.curContractSym().any().toSHErrLocFmt());
            dependentLabelMapping.put(contractSym.invoker().toSHErrLocFmt(), env.curContractSym().invoker().toSHErrLocFmt());

            ifFuncCallPcBefore = funcSym.externalPc();
            ifFuncCallPcAfter = funcSym.internalPc();
            ifFuncGammaLock = funcSym.callGamma();
        } else {
            // case 2: a(b) - local contract call or type cast
            funcName = ((Name) value).id;
//            if (funcName.equals(Utils.SUPER_KEYWORD)) {
//                funcName = Utils.genSuperName(env.curContractSym().getName());
//            }
            ifNamePc = Utils.getLabelNamePc(scopeContext.getSHErrLocName());

            if (!env.containsFunc(funcName)) {
                if (env.containsContract(funcName) || Utils.isPrimitiveType(funcName)) { 
                    // case 2a: type cast (terminating branch)
                    
                    if (args.size() != 1) {
                        throw new SemanticException("cast must have one argument", location);
                    }

                    String ifNameArgValue = argValueLabelNames.get(0);
                    Utils.contextFlow(env, psi.getNormalPath().c, endContext,
                            args.get(0).location);
                    // env.outContext = endContext;
                    if (!tail_position) {
                        env.cons.add(new Constraint(
                                new Inequality(psi.getNormalPath().c.lambda, beginContext.lambda),
                                env.hypothesis(), location, env.curContractSym().getName(),
                                Utils.ERROR_MESSAGE_LOCK_IN_NONLAST_OPERATION));
                    }
                    return new ExpOutcome(ifNameArgValue, psi);
                } else {
                    throw new SemanticException("method not found: " + funcName, location);
                }
            }

            // case 2b: local call

            funcSym = env.getFunc(funcName);

//            dependentLabelMapping.put(funcSym.sender().toSHErrLocFmt(), env.sender().toSHErrLocFmt());
            dependentLabelMapping.put(funcSym.sender().toSHErrLocFmt(), env.inContext.pc);

            ifFuncCallPcBefore = funcSym.externalPc();
            ifFuncCallPcAfter = funcSym.internalPc();
            ifFuncGammaLock = funcSym.callGamma();
        }

        // build hypothesis for sender and this
        // make sender equal to this
//        Inequality senderHypo = new Inequality(
//                funcSym.sender().toSHErrLocFmt(),
//                CompareOperator.Eq,
//                env.curContractSym.toSHErrLocFmt()
//        );
        // env.hypothesis().add(senderHypo);
        // ++createdHypoCount;

        // Evaluate callSpec
        if (externalCall && callSpec != null) {
            PathOutcome co = callSpec.genIFConstraints(env, false);
            psi.joinExe(co);
            env.inContext = Utils.genNewContextAndConstraints(env, false, co.getNormalPath().c, beginContext.lambda, callSpec.nextPcSHL(), callSpec.location);
        }
        
        // if external call and the target address is final, make callee.this equal to the target address
        if (externalCall) {
            if (externalTargetSym.isFinal) {
                dependentLabelMapping.put(
                        externalContractSym.thisSym().toSHErrLocFmt(),
                        externalTargetSym.toSHErrLocFmt());
            } else {
                dependentLabelMapping.put(
                        externalContractSym.thisSym().toSHErrLocFmt(),
                        ifContRtn); // XXX is this correct? Seems like this is the label of the contract value.
            }
        }

        // Generate constraints on arguments

//        System.err.println("Call: " + funcName);
        for (int i = 0; i < args.size(); ++i) {
            Expression arg = args.get(i);
            VarSym argSym = funcSym.parameters.get(i);
            if (argSym.isPrincipalVar()) {

                if (arg instanceof Name) {
                    VarSym valueSym = (VarSym) env.getVar(((Name) arg).id);
                    if (valueSym.isPrincipalVar()) {
//                        System.err.println("dependent " + argSym.typeSym + " -> " + valueSym.toSHErrLocFmt());
                        dependentLabelMapping.put(argSym.toSHErrLocFmt(), valueSym.toSHErrLocFmt());
                    }
                }
                // TODO: Do we throw exception if arg is not a Name?
            }

            // env.prevContext = prevContext = tmp;
            String ifNameArgValue = argValueLabelNames.get(i); // argument's label name
            Label ifArgLabel = funcSym.getLabelArg(i); // parameter's label
            assert ifArgLabel != null : argSym.getName();
            env.cons.add(
                    new Constraint(
                            new Inequality(
                                    ifNameArgValue,
                                    Relation.LEQ,
                                    ifArgLabel.toSHErrLocFmt(dependentLabelMapping)
                            ),
                            env.hypothesis(), arg.location, env.curContractSym().getName(),
                            "Input to the " + Utils.ordNumString(i + 1)
                                    + " argument must be trusted enough")
            ); // arglbl => paramlbl
            env.cons.add(
                    new Constraint(
                            new Inequality(
                                    ifNamePc,
                                    Relation.LEQ,
                                    ifArgLabel.toSHErrLocFmt(dependentLabelMapping)),
                            env.hypothesis(), arg.location, env.curContractSym().getName(),
                            "Current control flow must be trusted to feed the " + Utils.ordNumString(i + 1)
                            + "-th argument value")
            ); // pc => paramlbl

            if (argSym.typeSym instanceof ClosureTypeSym paramCs) {
                ClosureCreation.checkFlowInto(arg, paramCs, dependentLabelMapping, env, arg.location,
                        "Closure value flowing into the " + Utils.ordNumString(i + 1) + " argument must subtype its slot");
            }
        }

        // Generate pre-call and post-call constraints and contexts

        if (externalCall) {
//            String tem = ((Attribute) value).value.toSHErrLocFmt();
            env.cons.add(
                    new Constraint(
                            new Inequality(ifContRtn, ifFuncCallPcBefore.toSHErrLocFmt(dependentLabelMapping)),
                    env.hypothesis(), location, env.curContractSym().getName(),
                    "Target contract must be trusted to call this method")
                ); // target address..lbl => pc_ex
        }


        PathOutcome expPsi = new PathOutcome(new PsiUnit(new Context(
                Utils.joinLabels(psi.getNormalPath().c.pc, funcSym.endPc().toSHErrLocFmt(dependentLabelMapping)),
//                funcSym.getLabelNameCallGamma()
                ifFuncGammaLock.toSHErrLocFmt(dependentLabelMapping)
        ))); // post-call psi = (current normal path pc ⊔ function return label, gamma)

        for (Entry<ExceptionTypeSym, String> exp : funcSym.exceptions.entrySet()) {
            ExceptionTypeSym curSym = exp.getKey();
            String expLabelName = exp.getValue();
            // pc_2 dropped
            // expPsi.set(curSym, new PsiUnit(
            //         new Context(
            //                 Utils.joinLabels(expLabelName, funcSym.externalPcSLC()),
            //                 Utils.joinLabels(ifFuncGammaLock.toSHErrLocFmt(dependentLabelMapping),
            //                         ifFuncCallPcAfter.toSHErrLocFmt(dependentLabelMapping))),
            //         true)
            //     ); // exception-path post-call psi = (function exception-path pc ⊔ pc_ex,
            //         // gamma ⊔ pc_in)
            expPsi.set(curSym, new PsiUnit(
                    new Context(
                            Utils.joinLabels(expLabelName, funcSym.externalPcSLC()),
                            ifFuncGammaLock.toSHErrLocFmt(dependentLabelMapping)),
                    true)
                ); // exception-path post-call psi = (function exception-path pc ⊔ pc_ex, gamma)
                    // TODO: should pc label get joined by pc_ex, or psi.normal.pc?
                    //TODO: dependent
            //PsiUnit psiUnit = env.psi.get(curSym);
            //env.cons.add(new Constraint(new Inequality(Utils.makeJoin(expLabelName, ifNameFuncCallPcAfter), psiUnit.pc), env.hypothesis, location, env.curContractSym.name,
            //"Exception " + curSym.name + " is not trusted enough to throw"));
        }

        //TODO

        env.cons.add(
                new Constraint(new Inequality(ifNamePc, ifFuncCallPcBefore.toSHErrLocFmt(dependentLabelMapping)), env.hypothesis(),
                        location, env.curContractSym().getName(),
                        "Current control flow must be trusted to call this method")
                    ); // current pc => pc_ex
        // pc_2 dropped
        // env.cons.add(new Constraint(new Inequality(ifFuncCallPcBefore.toSHErrLocFmt(dependentLabelMapping),
        //         Utils.joinLabels(ifFuncCallPcAfter.toSHErrLocFmt(dependentLabelMapping), beginContext.lambda)), env.hypothesis(),
        //         location, env.curContractSym().getName(),
        //         "Calling this function does not respect static reentrancy locks")
        //     ); // pc_ex => pc_in ⊔ caller's lambda

        if (externalCall) {
            env.cons.add(new Constraint(
                    new Inequality(Utils.joinLabels(ifContRtn,
                            ifFuncGammaLock.toSHErrLocFmt(dependentLabelMapping)),
                            Relation.EQ, endContext.lambda), env.hypothesis(), location,
                    env.curContractSym().getName(),
                    "Calling this function does not respect static reentrancy locks")
                ); // target address..lbl ⊔ gamma == post-call lambda
        }

        if (!tail_position) {
//            env.cons.add(new Constraint(
//                    new Inequality(Utils.joinLabels(ifFuncCallPcAfter.toSHErrLocFmt(dependentLabelMapping), ifFuncGammaLock.toSHErrLocFmt(dependentLabelMapping)),
//                            Relation.EQ, endContext.lambda), env.hypothesis(), location,
//                    env.curContractSym().getName(),
//                    typecheck.Utils.ERROR_MESSAGE_LOCK_IN_NONLAST_OPERATION));

//            env.cons.add(new Constraint(
//                    new Inequality(psi.getNormalPath().c.lambda, beginContext.lambda),
//                    env.hypothesis(), location, env.curContractSym().getName(),
//                    typecheck.Utils.ERROR_MESSAGE_LOCK_IN_NONLAST_OPERATION));
            // apply the seq rule
        }

        // Finalize

        String ifNameFuncRtnValue = funcSym.rtn.toSHErrLocFmt(dependentLabelMapping);

        // realLabels
        if (funcSym.returnType instanceof ClosureTypeSym retCs) {
            this.realLabels = retCs.renderLabels(dependentLabelMapping);
        }

        psi.joinExe(expPsi);
        Utils.contextFlow(env, psi.getNormalPath().c, endContext, location);
        psi.setNormalPath(endContext);
        Constraint ln_to_t = new Constraint(new Inequality(psi.getNormalPath().c.pc, ifNameFuncRtnValue),
                env.hypothesis(), location, env.curContractSym().getName(), "Control flow after this call must be trusted by the function's return value label"
            ); // post-call pc (normal) => function return label(l_n)
        env.cons.add(ln_to_t);
        return new ExpOutcome(ifNameFuncRtnValue, psi);
    }

    /**
     * Reject the closure-invocation shapes whose code generation is not
     * implemented, rather than emitting partial code: results the
     * invocation would have to read back, and argument types with no
     * closure representation (see {@link compile.Utils#abiKind}).
     */
    private void checkInvokeSupported() {
        if (!closureSym.returnType.isVoid()) {
            throw new UnsupportedOperationException(
                    "non-void closure results not supported yet");
        }
        for (int i = 0; i < args.size(); ++i) {
            if (compile.Utils.abiKind(closureSym.unboundParams.get(i).getType())
                    == compile.Utils.AbiKind.UNSUPPORTED) {
                throw new UnsupportedOperationException(
                        "closure invoke arguments of this type not supported yet");
            }
        }
    }

    @Override
    public compile.ast.Expression solidityCodeGen(List<Statement> result, CompileEnv code) {
        if (isClosureInvoke) {
            assert value instanceof Attribute;
            compile.ast.Expression closureExp = ((Attribute) value).value.solidityCodeGen(result, code);

            int id = code.nextClosureTempId();
            String lExtBefName = "lExtBef" + id;
            String successName = "closureOk" + id;
            String ksName = "closureKs" + id;
            String valsName = "closureVals" + id;
            Map<String, String> boundPrincipals = new HashMap<>();

            checkInvokeSupported();

            if (!args.isEmpty()) {
                result.add(new VarDec(
                        new compile.ast.ArrayType(
                                new PrimitiveType(compile.Utils.PRIMITIVE_TYPE_UINT_NAME)),
                        ksName, new Literal("new uint256[](" + args.size() + ")")));
                result.add(new VarDec(
                        new compile.ast.ArrayType(
                                new PrimitiveType(compile.Utils.PRIMITIVE_TYPE_BYTES_NAME)),
                        valsName, new Literal("new bytes[](" + args.size() + ")")));
                for (int i = 0; i < args.size(); ++i) {
                    compile.ast.Expression argExp = args.get(i).solidityCodeGen(result, code);
                    VarSym binder = i < closureSym.binderSyms().size()
                            ? closureSym.binderSyms().get(i) : null;
                    compile.ast.Expression encoded = argExp;
                    if (binder != null && closurePcExLeaves.contains(binder.getName())) {
                        String argName = "closureArg" + id + "_" + i;
                        boolean isAddress = compile.Utils.PRIMITIVE_TYPE_ADDRESS_NAME
                                .equals(closureSym.unboundParams.get(i).getType().solCode());
                        result.add(new VarDec(
                                new PrimitiveType(compile.Utils.PRIMITIVE_TYPE_ADDRESS_NAME),
                                argName,
                                isAddress ? argExp
                                        : new compile.ast.Call(
                                                compile.Utils.PRIMITIVE_TYPE_ADDRESS_NAME,
                                                List.of(argExp))));
                        boundPrincipals.put(binder.getName(), argName);
                        encoded = new SingleVar(argName);
                    }
                    result.add(new Assign(
                            new compile.ast.Subscript(new SingleVar(ksName),
                                    new Literal(String.valueOf(i))),
                            new Literal(String.valueOf(i))));
                    result.add(new Assign(
                            new compile.ast.Subscript(new SingleVar(valsName),
                                    new Literal(String.valueOf(i))),
                            new compile.ast.Call("abi.encode", List.of(encoded))));
                }
            }

            result.addAll(code.buildLExtBef(lExtBefName, closurePcExLeaves, boundPrincipals));

            result.add(new VarDec(
                    new PrimitiveType(compile.Utils.PRIMITIVE_TYPE_BOOL_NAME), successName));
            compile.ast.Call invokeCall = args.isEmpty()
                    ? new compile.ast.Call(compile.Utils.RUNTIME_FUNC_INVOKE,
                            List.of(closureExp, new SingleVar(lExtBefName)))
                    : new compile.ast.Call(compile.Utils.RUNTIME_FUNC_INVOKE_WITH,
                            List.of(closureExp, new SingleVar(ksName),
                                    new SingleVar(valsName), new SingleVar(lExtBefName)));
            result.add(new Assign(
                    List.of(new SingleVar(successName), new SingleVar("")), invokeCall));
            code.markClosureStructRequired();
            code.markClosureLibRequired();
            return new compile.ast.Call("assert", List.of(new SingleVar(successName)));
        }
        List<compile.ast.Expression> argumentExps = new ArrayList<>();
        for (Expression arg: args) {
            argumentExps.add(arg.solidityCodeGen(result, code));
        }
        compile.ast.Call callExp;
        String funcName;
        // hash the name if not private method
        if (builtIn || isCast) {
            funcName = value instanceof Name ? ((Name) value).id : ((Attribute) value).attr.id;
        } else {
            assert funcSym != null;
            if (!(value instanceof Name))  {// || funcSym.isPublic()) {
                // external call
                funcName = Utils.methodNameHash(funcSym.funcName, funcSym.plainSignature());
            } else {
                funcName = funcSym.funcName;
            }
        }

        if (value instanceof Name) {
            // internal call
            callExp = new compile.ast.Call(funcName, argumentExps);
        } else {
            // external call
            assert value instanceof Attribute;
            compile.ast.Expression target = ((Attribute) value).value.solidityCodeGen(result, code);
            // String funcName = ((Attribute) value).attr.id;
            if (builtIn && funcName.equals("length")) {
                return new Attr(target, funcName);
            }
            callExp = new ExternalCall(target, funcName, argumentExps, callSpec != null ? callSpec.solidityCodeGen(result, code) : null);
        }
        assert ntced : "funcSym being null: " + callExp.toSolCode() + " " + builtIn;
        assert !(funcName.equals("send") && !builtIn);
        if (builtIn) {
            return compile.Utils.translateBuiltInFunc(callExp);
        } else if (isCast || funcSym.exceptions.size() == 0) {
            return callExp;
        } else {
            // statVar, dataVar = call(...);
            // if (statVar != 0) return statVar, dataVar
            // else tempVar = parse(dataVar);
            // replace call with tempVar
            SingleVar statVar = new SingleVar(code.newTempVarName());
            SingleVar dataVar = new SingleVar(code.newTempVarName());
            result.add(new VarDec(compile.Utils.PRIMITIVE_TYPE_UINT, statVar.name()));
            result.add(new VarDec(compile.Utils.PRIMITIVE_TYPE_BYTES, dataVar.name()));
            SingleVar tempVar = null;
            if (!funcSym.returnType.isVoid()) {
                tempVar = new SingleVar(code.newTempVarName());
                result.add(new VarDec(new PrimitiveType(funcSym.returnType.getName()), tempVar.name()));
            }
            result.add(new Assign(
                    List.of(statVar, dataVar),
                    callExp
            ));

            // map exceptionID
            IfStatement mapExpIds = null;
            int i = 1;
            for (Entry<ExceptionTypeSym, String> entry: funcSym.exceptions.entrySet()) {
                IfStatement ifexp = new IfStatement(
                        new BinaryExpression(compile.Utils.SOL_BOOL_EQUAL, statVar, new Literal(String.valueOf(i))),
                        List.of(new Assign(statVar, new Literal(String.valueOf(code.getExceptionId(entry.getKey()))))),
                        mapExpIds == null ? null : List.of(mapExpIds)
                        );
                mapExpIds = ifexp;
                ++i;
            }
            if (mapExpIds != null) {
                result.add(mapExpIds);
            }

            compile.ast.Expression condition = new BinaryExpression(compile.Utils.SOL_BOOL_NONEQUAL,
                    statVar, new Literal(compile.Utils.RETURNCODE_NORMAL));
            IfStatement test = new IfStatement(condition,
                    List.of(new Return(List.of(statVar, dataVar))));
            result.add(test);

            if (funcSym.returnType.isVoid()) {
                return new Pass();
            } else {
                result.add(new Assign(tempVar,
                        code.decodeVars(
                                funcSym.parameters.stream()
                                        .map(para -> new PrimitiveType(para.typeSym.getName()))
                                        .collect(
                                                Collectors.toList()),
                                dataVar)));
                return tempVar;
            }
        }
    }

//    public String toSolCode() {
//        // logger.debug("toSOl: Call");
//        String funcName = value.toSolCode();
////        if (Utils.isBuiltinFunc(funcName)) {
////            return Utils.transBuiltinFunc(funcName, this);
////        }
//        String argsCode = "";
//        boolean first = true;
//        for (Expression exp : args) {
//            if (!first) {
//                argsCode += ", ";
//            } else {
//                first = false;
//            }
//            argsCode += exp.toSolCode();
//        }
//
//        return CompileEnv.toFunctionCall(funcName, argsCode);
//    }

    @Override
    public List<Node> children() {
        List<Node> rtn = new ArrayList<>();
        rtn.add(value);
        if (callSpec != null) rtn.add(callSpec);
        rtn.addAll(args);
        return rtn;
    }

    @Override
    public boolean typeMatch(Expression expression) {
        if (!(expression instanceof Call &&
                super.typeMatch(expression))) {
            return false;
        }

        Call c = (Call) expression;

        boolean bothArgsNull = c.args == null && args == null;

        if (!bothArgsNull) {
            if (args == null || c.args == null || args.size() != c.args.size()) {
                return false;
            }
            int index = 0;
            while (index < args.size()) {
                if (!args.get(index).typeMatch(c.args.get(index))) {
                    return false;
                }
                ++index;
            }
        }

        return true;
    }

    /**
     * Check if a type cast
     */
    public boolean isCast(VisitEnv env) {
        if (value instanceof Name) {
            // a(b)
            String funcName = ((Name) value).id;
            Sym s = env.getCurSym(funcName);
            assert s != null;
            if (!(s instanceof FuncSym)) {
                if (s instanceof InterfaceSym || s instanceof BuiltinTypeSym) {
                    return true;
                }
                assert false;
                return false;
            } else {
                return false;
            }
        } else {
            return false;
        }
    }

    public boolean isExternal() {
        return value instanceof Attribute;
    }

    @Override
    public java.util.Map<String, compile.ast.Type> readMap(CompileEnv code) {
        Map<String, Type> result = new HashMap<>();
        for (Expression arg: args) {
            result.putAll(arg.readMap(code));
        }
        return result;
    }

    public void checkAndRenameSuper(String extendsContractName) {
        if (value instanceof Name name) {
            if (name.id.equals(Utils.SUPER_KEYWORD)) {
                name.id = Utils.genSuperName(extendsContractName);
            }
        }
    }
}

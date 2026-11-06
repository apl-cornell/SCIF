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
import typecheck.sherrlocUtils.Relation;
import typecheck.*;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

public class Call extends TrailerExpr {

    List<Expression> args;


    /**
     * The method this call invokes, as resolved by the regular type check, or null if it
     * invokes none. Code generation uses it; the IFC check resolves the callee separately.
     */
    FuncSym funcSym = null;
    /**
     * The node this expression resolves to when it is not a call, such as a cast, as determined
     * by the regular type check; every later pass is its. Null for a call.
     */
    private Expression form = null;
    boolean builtIn = false, ntced = false;
    CallSpec callSpec;

    // Store the IFC-type-checked labels (in callee's names), null if not applicable
    private ClosureTypeSym.RealLabels realLabels;

    ClosureTypeSym.RealLabels realLabels() {
        return realLabels;
    }

    /** Whether this call invokes a closure, as determined by the regular type check. */
    private boolean isClosureInvoke = false;
    /**
     * For a closure invocation, the principals in the closure's external begin label (lextbef),
     * set by the IFC check. Code generation requires it.
     */
    private List<String> closurePcExLeaves = null;
    /**
     * For a closure invocation, the invoked closure's declared type with its labels, set by the
     * IFC check. Code generation requires it.
     */
    private ClosureTypeSym closureSym = null;
    /** The type of this call's result, once it has been IFC-checked. */
    private TypeSym resultType;

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
            funcName = att.attr.id; // b
            if (att.value instanceof Name n && env.getCurSym(n.id) == null) {
                throw new SemanticException("variable not found: " + n.id, att.value.location);
            }
            // The receiver is a variable, or a member or element of one.
            VarSym varSym = att.value.getVarInfo(env);
            if (varSym == null) {
                throw new SemanticException(
                        "the receiver of a method call must be a variable, a member, or an element",
                        att.value.location);
            }
            if (varSym.typeSym instanceof ArrayTypeSym) {
                return resolveTo(new ArrayOperation(att.value, funcName, args), env, parent);
            }
            if (!(att.value instanceof Name)) {
                att.value.genTypeConstraints(env, now);
            }
            String varName = varSym.getName();
            Sym s;

            if (varSym.typeSym instanceof InterfaceSym contractSym) {
                s = contractSym.getFunc(funcName);
                if (s == null)
                    throw new SemanticException("function " + varName + "." + funcName + "() not found",
                        location);
                funcSym = (FuncSym) s;

            } else if (varSym.typeSym instanceof ClosureTypeSym closureTypeSym) {
                // c.invoke(args): args must match the still-unbound parameters; the call's type is the closure's return.
                this.builtIn = true;
                this.isClosureInvoke = true;
                if (!funcName.equals("invoke")) {
                    throw new SemanticException("type error: unknown closure operator " + funcName, this.location);
                }
                CallArguments.genTypeConstraints(args, closureTypeSym.unboundParams,
                        "closure " + varName, env, now, location);
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
                    if (args.size() != 1) {
                        throw new SemanticException("cast must have one argument", location);
                    }
                    return resolveTo(new Cast((Name) value, args.get(0)), env, parent);
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
        this.funcSym = funcSym;

        if (extern && callSpec != null) {
            callSpec.genTypeConstraints(env, now);
        }
        CallArguments.genTypeConstraints(args,
                funcSym.parameters.stream().map(p -> p.typeSym).toList(),
                "method " + funcName, env, now, location);
        String rtnTypeName = funcSym.returnType.toSHErrLocFmt();
        env.addCons(now.genTypeConstraints(rtnTypeName, Relation.EQ, env, location));

        for (Map.Entry<ExceptionTypeSym, Label> tl : funcSym.exceptions.entrySet()) {
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
    protected ExpOutcome genFlowConstraints(VisitEnv env, boolean tail_position)
            throws SemanticException {
        if (form != null) {
            return form.genIFConstraints(env, tail_position);
        }
        ExpOutcome result = callIFConstraints(env, tail_position);
        // A call may assign to any field, so fields hold new principals afterwards.
        env.havocFields();
        return result;
    }

    private ExpOutcome callIFConstraints(VisitEnv env, boolean tail_position)
            throws SemanticException {
        //TODO: Assuming value is a Name for now
        Context beginContext = env.inContext; // (pc, lambda)
        Context endContext = new Context(Utils.getLabelNamePc(toSHErrLocFmt()),
                Utils.getLabelNameLock(toSHErrLocFmt()));
        Map<String, String> dependentLabelMapping = new HashMap<>();

        PathOutcome psi = new PathOutcome(new PsiUnit(endContext));
        CallArguments.Evaluated evaluated = CallArguments.evaluate(args, env, psi);

        String funcName;
        String ifNamePc; // currentMethod.PC
        FuncSym calleeSym;
        String namespace = "";
        Label ifFuncCallPcBefore, ifFuncGammaLock;

        ExpOutcome vo = null;

        // The receiver of the call: the contract whose method is called, the label of the
        // reference to it, and the principal it holds. An internal call's receiver is this
        // contract itself.
        InterfaceSym calleeContract;
        Callee callee;
        String ifContRtn = null;
        String receiverPrincipal = null; // the principal the receiver holds, if it is a variable
        // The principal the callee's sender stands for.
        String senderPrincipal;

        if (!(value instanceof Name)) {
            if (!(value instanceof Attribute att)) {
                throw new Error("Internal compiler error" + location.errString());
            }

            // case 1: a.b(c) where a is a contract or an array, b is a function and c are the arguments
            // att = a.b

            // evaluate a.b
            vo = att.value.genIFConstraints(env, false);
            psi.joinExe(vo.psi);
            ifContRtn = vo.valueLabelName; // a..lbl
            receiverPrincipal = att.value.principalName(env);

            funcName = (att.attr).id; // b
            TypeSym receiverType = att.value.type();

            if (receiverType instanceof ClosureTypeSym cs) {
                // case 1b: a is a closure value; a.invoke(args) calls the closure's target, with
                // the closure's labels, which are already in this contract's terms.
                if (!funcName.equals("invoke")) {
                    throw new SemanticException(
                            "type error: unknown closure operator " + funcName, location);
                }
                this.closureSym = cs;
                this.closurePcExLeaves = cs.pcEx().principalLeaves();
                cs.registerAtoms(env);
                // An invoke argument fills the binder its position names in the closure's
                // labels, so if the labels name it, its principal must be known.
                List<VarSym> binders = cs.binderSyms();
                for (int i = 0; i < args.size() && i < binders.size(); i++) {
                    VarSym binder = binders.get(i);
                    if (binder != null && evaluated.principals().get(i) == null
                            && cs.referencesBinder(binder)) {
                        throw new SemanticException(
                                "must use an address or contract variable for the "
                                        + Utils.ordNumString(i + 1)
                                        + " invoke argument: the closure's labels depend on it",
                                args.get(i).location);
                    }
                }
                ifNamePc = Utils.getLabelNamePc(scopeContext.getSHErrLocName());
                callee = Callee.ofClosure(cs, env.curContractSym().invoker());
                calleeContract = null;
                // The closure's target is called with this contract as its invoker.
                senderPrincipal = env.thisSym().toSHErrLocFmt();
            } else {
                // case 1c: external call
                TypeSym conType = receiverType;
                namespace = conType.getName();
                env.addSigReq(namespace, conType.getName());
                ifNamePc = Utils.getLabelNamePc(scopeContext.getSHErrLocName());
                calleeContract = env.getContract(conType.getName());
                calleeSym = calleeContract.getFunc(funcName);
                if (calleeSym == null)
                    throw new SemanticException("not found: " + conType.getName() + "." + funcName, location);
                callee = Callee.ofMethod(calleeSym, calleeContract);
                // The callee's sender is this contract.
                senderPrincipal = env.thisSym().toSHErrLocFmt();
            }
        } else {
            // case 2: a(b) - local contract call or type cast
            funcName = ((Name) value).id;
//            if (funcName.equals(Utils.SUPER_KEYWORD)) {
//                funcName = Utils.genSuperName(env.curContractSym().getName());
//            }
            ifNamePc = Utils.getLabelNamePc(scopeContext.getSHErrLocName());

            if (!env.containsFunc(funcName)) {
                throw new SemanticException("method not found: " + funcName, location);
            }

            // case 2b: internal call, whose receiver is this contract

            calleeSym = env.getFunc(funcName);
            calleeContract = env.curContractSym();
            callee = Callee.ofMethod(calleeSym, calleeContract);
            ifContRtn = env.thisSym().toSHErrLocFmt();
            receiverPrincipal = env.thisSym().toSHErrLocFmt();
            // The callee runs with the current sender, so its demands on the sender are met
            // with the authority of the current control flow.
            senderPrincipal = env.inContext.pc;
        }

        if (calleeContract != null) {
            dependentLabelMapping.put(calleeContract.any().toSHErrLocFmt(),
                    env.curContractSym().any().toSHErrLocFmt());
            dependentLabelMapping.put(calleeContract.invoker().toSHErrLocFmt(),
                    env.curContractSym().invoker().toSHErrLocFmt());
        }
        dependentLabelMapping.put(callee.sender(), senderPrincipal);
        ifFuncCallPcBefore = callee.externalPc();
        ifFuncGammaLock = callee.gamma();

        // build hypothesis for sender and this
        // make sender equal to this
//        Inequality senderHypo = new Inequality(
//                calleeSym.sender().toSHErrLocFmt(),
//                CompareOperator.Eq,
//                env.curContractSym.toSHErrLocFmt()
//        );
        // env.hypothesis().add(senderHypo);
        // ++createdHypoCount;

        // Evaluate callSpec
        if (callSpec != null) {
            PathOutcome co = callSpec.genIFConstraints(env, false);
            psi.joinExe(co);
            env.inContext = Utils.genNewContextAndConstraints(env, false, co.getNormalPath().c, beginContext.lambda, callSpec.nextPcSHL(), callSpec.location);
        }
        
        // The callee's labels are instantiated differently in input and output positions.
        // Its inputs (argument labels, external pc) are demands it makes of this caller,
        // so there the callee's `this` is the callee itself: the principal the receiver
        // holds. This contract can meet those demands only with what it trusts itself,
        // since it is the top principal here. Its outputs (return label, post-call pc,
        // lock label) are promises trusted only as much as the callee, which acts for the
        // label of the reference used to call it, so there its `this` becomes that label.
        // A principal parameter of the callee names the principal passed for it. If that
        // is unknown, the callee's demands are taken to name the most trusted principal and
        // its promises the least trusted one.
        Map<String, String> inputMapping = new HashMap<>(dependentLabelMapping);
        Map<String, String> outputMapping = new HashMap<>(dependentLabelMapping);
        CallArguments.mapPrincipals(callee.principalParams(), evaluated,
                env.curContractSym().top().toSHErrLocFmt(), inputMapping);
        CallArguments.mapPrincipals(callee.principalParams(), evaluated,
                env.curContractSym().any().toSHErrLocFmt(), outputMapping);
        if (callee.thisSym() != null) {
            inputMapping.put(callee.thisSym(), receiverPrincipal != null
                    ? receiverPrincipal : env.thisSym().toSHErrLocFmt());
            outputMapping.put(callee.thisSym(), ifContRtn);
        }
        // Whatever the callee's labels say, its results are trusted no more than the
        // reference used to call it.
        String calleeTrust = ifContRtn;

        // Generate constraints on arguments

        CallArguments.checkFlowInto(args, evaluated, ifNamePc, callee.paramLabels(),
                inputMapping, "", env);
        for (int i = 0; i < args.size(); ++i) {
            Expression arg = args.get(i);
            if (callee.paramTypes().get(i) instanceof ClosureTypeSym paramCs) {
                ClosureCreation.checkFlowInto(arg, paramCs, inputMapping, env, arg.location,
                        "Closure value flowing into the " + Utils.ordNumString(i + 1) + " argument must subtype its slot");
            }
        }

        // Generate pre-call and post-call constraints and contexts

        env.checkFlow(ifContRtn, ifFuncCallPcBefore.toSHErrLocFmt(inputMapping),
                location, "Target contract must be trusted to call this method"); // target address..lbl => pc_ex


        String postCallLock = attenuate(ifFuncGammaLock.toSHErrLocFmt(outputMapping), calleeTrust);
        PathOutcome expPsi = new PathOutcome(new PsiUnit(new Context(
                attenuate(Utils.joinLabels(psi.getNormalPath().c.pc,
                        callee.returnLabel().toSHErrLocFmt(outputMapping)), calleeTrust),
                postCallLock
        ))); // post-call psi = (current normal path pc ⊔ function end pc, gamma), attenuated

        checkExceptionData(env, callee.exceptions().keySet(), outputMapping);
        for (Entry<ExceptionTypeSym, Label> exp : callee.exceptions().entrySet()) {
            // exception-path post-call psi = (current pc ⊔ declared exception label, gamma),
            // attenuated like the normal path
            expPsi.set(exp.getKey(), new PsiUnit(
                    new Context(
                            attenuate(Utils.joinLabels(psi.getNormalPath().c.pc,
                                    exp.getValue().toSHErrLocFmt(outputMapping)), calleeTrust),
                            postCallLock),
                    true));
        }

        //TODO

        env.checkFlow(ifNamePc, ifFuncCallPcBefore.toSHErrLocFmt(inputMapping), location,
                "Current control flow must be trusted to call this method"); // current pc => pc_ex
        // pc_2 dropped
        // env.cons.add(new Constraint(new Inequality(ifFuncCallPcBefore.toSHErrLocFmt(dependentLabelMapping),
        //         Utils.joinLabels(ifFuncCallPcAfter.toSHErrLocFmt(dependentLabelMapping), beginContext.lambda)), env.hypothesis(),
        //         location, env.curContractSym().getName(),
        //         "Calling this function does not respect static reentrancy locks")
        //     ); // pc_ex => pc_in ⊔ caller's lambda

        // callee trust ⊔ gamma == post-call lambda
        env.checkEqual(postCallLock, endContext.lambda, location,
                "Calling this function does not respect static reentrancy locks");

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

        String ifNameFuncRtnValue = attenuate(callee.returnLabel().toSHErrLocFmt(outputMapping),
                calleeTrust);

        // realLabels
        if (callee.returnType() instanceof ClosureTypeSym retCs) {
            this.realLabels = retCs.renderLabels(outputMapping);
        }

        psi.joinExe(expPsi);
        Utils.contextFlow(env, psi.getNormalPath().c, endContext, location);
        psi.setNormalPath(endContext);
        // post-call pc (normal) => function return label (l_n)
        env.checkFlow(psi.getNormalPath().c.pc, ifNameFuncRtnValue, location,
                "Control flow after this call must be trusted by the function's return value label");
        resultType = callee.returnType();
        return new ExpOutcome(ifNameFuncRtnValue, psi);
    }

    /**
     * Checks that the data of each exception in {@code exceptions}, which the callee may throw, is
     * trusted at the labels this contract's declaration of that exception gives it, since
     * handlers here read it at those labels. The callee's labels are instantiated with
     * {@code outputMapping}, with its exception's principal parameters standing for the
     * corresponding parameters of this contract's declaration.
     */
    private void checkExceptionData(VisitEnv env, java.util.Set<ExceptionTypeSym> exceptions,
            Map<String, String> outputMapping) {
        // A handler reads each field at its label joined with the label of the exception's
        // path, which is attenuated by the callee's trust, so the data is too.
        for (ExceptionTypeSym calleeExp : exceptions) {
            ExceptionTypeSym localExp = env.getExp(calleeExp.getName());
            if (localExp == null) {
                throw new AssertionError("exception " + calleeExp.getName()
                        + " of an external call has no local declaration");
            }
            Map<String, String> mapping = new HashMap<>(outputMapping);
            for (int i = 0; i < calleeExp.parameters().size(); ++i) {
                VarSym calleeParam = calleeExp.parameters().get(i);
                if (calleeParam.isPrincipalTyped()) {
                    mapping.put(calleeParam.toSHErrLocFmt(), env.freshPrincipal(
                            localExp.parameters().get(i).toSHErrLocFmt()));
                }
            }
            for (int i = 0; i < calleeExp.parameters().size(); ++i) {
                VarSym calleeParam = calleeExp.parameters().get(i);
                VarSym localParam = localExp.parameters().get(i);
                env.checkFlow(calleeParam.ifl.toSHErrLocFmt(mapping),
                        localParam.ifl.toSHErrLocFmt(), location,
                        "Data of exception " + calleeExp.getName()
                                + " from this call must be trusted at its declared label here");
            }
        }
    }

    /**
     * What the call rule needs to know about a callee, whether a method or a closure's target.
     *
     * @param paramLabels the label of each parameter
     * @param principalParams the principal each parameter binds, or null for a parameter that
     *        binds none
     * @param paramTypes the type of each parameter
     * @param externalPc the label the caller's control flow must be trusted at
     * @param gamma the lock label of the call
     * @param returnLabel the label of the result, which is also the end pc
     * @param returnType the type of the result
     * @param exceptions the exceptions the callee may throw, each with its label
     * @param sender the SHErrLoc name of the principal that stands for the caller in the
     *        callee's labels
     * @param thisSym the SHErrLoc name of the callee's {@code this}, or null if its labels are
     *        already in this contract's terms
     */
    private record Callee(List<Label> paramLabels, List<VarSym> principalParams,
            List<TypeSym> paramTypes, Label externalPc, Label gamma, Label returnLabel,
            TypeSym returnType, Map<ExceptionTypeSym, Label> exceptions, String sender,
            String thisSym) {

        /** The callee for a call of method {@code method} of contract {@code contract}. */
        static Callee ofMethod(FuncSym method, InterfaceSym contract) {
            List<Label> labels = new ArrayList<>();
            List<VarSym> principals = new ArrayList<>();
            List<TypeSym> types = new ArrayList<>();
            for (int i = 0; i < method.parameters.size(); ++i) {
                VarSym param = method.parameters.get(i);
                labels.add(method.getLabelArg(i));
                principals.add(param.isPrincipalTyped() ? param : null);
                types.add(param.typeSym);
            }
            return new Callee(labels, principals, types, method.externalPc(), method.callGamma(),
                    method.endPc(), method.returnType, method.exceptions,
                    method.sender().toSHErrLocFmt(), contract.thisSym().toSHErrLocFmt());
        }

        /**
         * The callee for an invocation of a closure of type {@code type}, whose labels name
         * the closure's invoker as {@code invoker}.
         */
        static Callee ofClosure(ClosureTypeSym type, VarSym invoker) {
            List<Label> labels = new ArrayList<>();
            for (int i = 0; i < type.unboundParams.size(); ++i) {
                labels.add(type.getLabelArg(i));
            }
            return new Callee(labels, type.binderSyms(), type.unboundParams, type.pcEx(),
                    type.callGamma(), type.endPc(), type.returnType, Map.of(),
                    invoker.toSHErrLocFmt(), null);
        }
    }

    /**
     * The label {@code label} trusted no more than {@code trust}, or {@code label} itself if
     * {@code trust} is null. {@code trust} may be null.
     */
    private static String attenuate(String label, String trust) {
        return trust == null ? label : Utils.joinLabels(label, trust);
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
        if (form != null) {
            return form.solidityCodeGen(result, code);
        }
        if (isClosureInvoke) {
            if (closureSym == null || closurePcExLeaves == null) {
                throw new AssertionError(
                        "code generation for a closure invocation requires the IFC check: "
                                + location);
            }
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
        if (builtIn) {
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
            callExp = new ExternalCall(target, funcName, argumentExps, callSpec != null ? callSpec.solidityCodeGen(result, code) : null);
        }
        assert ntced : "funcSym being null: " + callExp.toSolCode() + " " + builtIn;
        assert !(funcName.equals("send") && !builtIn);
        if (builtIn) {
            return compile.Utils.translateBuiltInFunc(callExp);
        } else if (funcSym.exceptions.size() == 0) {
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
            for (Entry<ExceptionTypeSym, Label> entry: funcSym.exceptions.entrySet()) {
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

    @Override
    public String principalName(VisitEnv env) {
        return form != null ? form.principalName(env) : null;
    }

    public boolean isExternal() {
        return value instanceof Attribute;
    }

    @Override
    public java.util.Map<String, compile.ast.Type> readMap(CompileEnv code) {
        if (form != null) {
            return form.readMap(code);
        }
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

    @Override
    protected TypeSym staticType(VisitEnv env) {
        return form != null ? form.type() : resultType;
    }

    /**
     * Resolves this expression to {@code form}, which takes its place in every later pass, and
     * generates the type constraints of {@code form} in the scope context {@code parent}.
     */
    private ScopeContext resolveTo(Expression form, NTCEnv env, ScopeContext parent)
            throws SemanticException {
        this.form = form;
        form.setLoc(location);
        form.scopeContext = scopeContext;
        return form.genTypeConstraints(env, parent);
    }
}

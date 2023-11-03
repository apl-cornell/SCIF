package ast;

import compile.CompileEnv;
import compile.ast.Assign;
import compile.ast.Attr;
import compile.ast.BinaryExpression;
import compile.ast.ExternalCall;
import compile.ast.IfStatement;
import compile.ast.Literal;
import compile.ast.PrimitiveType;
import compile.ast.Return;
import compile.ast.SingleVar;
import compile.ast.Statement;
import compile.ast.Subscript;
import compile.ast.Type;
import compile.ast.VarDec;
import java.util.List;
import java.util.Map.Entry;
import java.util.stream.Collectors;
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

    public Call() {
        this.args = new ArrayList<>();
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

    public Expression getArgAt(int index) {
        return args.get(index);
    }

    public ScopeContext ntcGenCons(NTCEnv env, ScopeContext parent) {
        this.ntced = true;
        ScopeContext now = new ScopeContext(this, parent);
        String funcName;
        FuncSym funcSym;
        boolean extern = false;
        if (!(value instanceof Name)) {
            if (value instanceof Attribute) {
                // a.b(c), a must be a contract or an array
                extern = true;
                Attribute att = (Attribute) value;
                String varName = ((Name) att.value).id;
                funcName = att.attr.id;
                Sym s = env.getCurSym(varName);
                logger.debug("var " + varName + ": " + s.getName());
                if (s instanceof VarSym varSym) {
                    if (varSym.typeSym instanceof InterfaceSym contractSym) {
                        s = contractSym.getFunc(funcName);
                        assert s != null: "func in " + varName + "." + funcName + "() not found";

                        funcSym = (FuncSym) s;
                    } else if (varSym.typeSym instanceof ArrayTypeSym arrayTypeSym) {
                        // TODO: change the hard-code style
                        TypeSym arrayTSym = arrayTypeSym.valueType;
                        String arrayTName = arrayTSym.toSHErrLocFmt();
                        this.builtIn = true;
                        if (funcName.equals("pop")) {
                            // return T
                            assert args.size() == 0: "type error";
                            env.addCons(now.genCons(arrayTName, Relation.EQ, env, location));
                            return now;
                        } else if (funcName.equals("push")) {
                            // require one T, return void
                            assert args.size() == 1: "type error";
                            Expression arg = args.get(0);
                            ScopeContext argContext = arg.ntcGenCons(env, now);
                            env.addCons(argContext.genCons(arrayTName, Relation.GEQ, env, arg.location));
                            TypeSym rtnTypeSym = (TypeSym) env.getSym(BuiltInT.VOID);
                            env.addCons(now.genCons(rtnTypeSym.toSHErrLocFmt(), Relation.EQ, env, location));
                            return now;
                        } else if (funcName.equals("length")) {
                            // return uint
                            assert args.size() == 0: "type error";
                            TypeSym rtnTypeSym = (TypeSym) env.getSym(BuiltInT.UINT);
                            env.addCons(now.genCons(rtnTypeSym.toSHErrLocFmt(), Relation.EQ, env, location));
                            return now;
                        } else {
                            assert false: "type error";
                            return null;
                        }
                    } else {
                        assert false: "type error: " + varName + "." + funcName + "() " + varSym.typeSym.toSHErrLocFmt() + (varSym.typeSym instanceof ContractSym);
                        return null;
                    }
                } else {
                    assert false;
                    return null;
                }
            } else {
                assert false;
                return null;
            }
        } else {
            // a(b)
            funcName = ((Name) value).id;
            Sym s = env.getCurSym(funcName);
            if (s == null) {
                assert false: "method not found: " + funcName;
                return null;
            }
            if (!(s instanceof FuncSym)) {
                if (s instanceof ContractSym || s instanceof BuiltinTypeSym) {
                    env.addCons(now.genCons(s.getName(), Relation.EQ, env, location));
                    isCast = true;
                    return now;
                }
                assert false: "contract not found: " + s.getName();
                return null;
            }
            funcSym = ((FuncSym) s);
            if (funcSym.isBuiltIn()) {
                this.builtIn = true;
            }
        }
        this.funcSym = funcSym;
        // typecheck arguments
        for (int i = 0; i < args.size(); ++i) {
            Expression arg = args.get(i);
            TypeSym paraInfo = funcSym.parameters.get(i).typeSym;
            ScopeContext argContext = arg.ntcGenCons(env, now);
            String typeName = paraInfo.toSHErrLocFmt();
            env.addCons(argContext.genCons(typeName, Relation.GEQ, env, arg.location));
        }
        String rtnTypeName = funcSym.returnType.toSHErrLocFmt();
        env.addCons(now.genCons(rtnTypeName, Relation.EQ, env, location));

        for (Map.Entry<ExceptionTypeSym, String> tl : funcSym.exceptions.entrySet()) {
            if (!parent.isCheckedException(tl.getKey(), extern)) {
                System.err.println(
                        "Unchecked exception: " + tl.getKey().getName() + " at " + location.toString());
                throw new RuntimeException();
            }
        }
        return now;
    }

    @Override
    public ExpOutcome genConsVisit(VisitEnv env, boolean tail_position) {
        //TODO: Assuming value is a Name for now
        Context beginContext = env.inContext;
        Context endContext = new Context(typecheck.Utils.getLabelNamePc(toSHErrLocFmt()),
                typecheck.Utils.getLabelNameLock(toSHErrLocFmt()));
        Map<String, String> dependentLabelMapping = new HashMap<>();

        List<String> argValueLabelNames = new ArrayList<>();

        PathOutcome psi = new PathOutcome(new PsiUnit(endContext));
        ExpOutcome ao = null;

        for (Expression arg : args) {
            ao = arg.genConsVisit(env, false);
            psi.joinExe(ao.psi);
            argValueLabelNames.add(ao.valueLabelName);
            env.inContext = new Context(
                    Utils.joinLabels(ao.psi.getNormalPath().c.pc, beginContext.pc),
                    beginContext.lambda);
        }

        String funcName;
        String ifNamePc;
        FuncSym funcSym;
        String namespace = "";
        Label ifFuncCallPcBefore, ifFuncCallPcAfter, ifFuncGammaLock;

        ExpOutcome vo = null;

        boolean externalCall = false;
        InterfaceSym externalContractSym = null;
        VarSym externalTargetSym = null;
        String ifContRtn = null;
        if (!(value instanceof Name)) {
            if (value instanceof Attribute) {
                //  the case: a.b(c) where a is a contract or an array, b is a function and c are the arguments
                // att = a.b

                externalCall = true;
                Attribute att = (Attribute) value;
                vo = att.value.genConsVisit(env, false);
                psi.joinExe(vo.psi);
                ifContRtn = vo.valueLabelName;

                //TODO: assuming a's depth is 1
                funcName = (att.attr).id;
                String varName = ((Name) att.value).id;
                VarSym var = env.getVar(varName);
                if (var.typeSym instanceof ArrayTypeSym arrayTypeSym) {
                    //TODO: change the hard-code style

                    TypeSym arrayTSym = arrayTypeSym.valueType;
                    String arrayTName = arrayTSym.toSHErrLocFmt();
                    if (funcName.equals("pop")) {
                        // requires pc => integrity of the array var
                        Utils.contextFlow(env, psi.getNormalPath().c, endContext, location);
                        ifNamePc = Utils.getLabelNamePc(scopeContext.getSHErrLocName());
                        env.cons.add(
                                new Constraint(new Inequality(ifNamePc, ifContRtn), env.hypothesis(),
                                        location,
                                        "Current control flow must be trusted to call this method"));
                        if (!tail_position) {
                            env.cons.add(new Constraint(
                                    new Inequality(psi.getNormalPath().c.lambda, beginContext.lambda),
                                    env.hypothesis(), location,
                                    typecheck.Utils.ERROR_MESSAGE_LOCK_IN_NONLAST_OPERATION));
                        }
                        return new ExpOutcome(ifNamePc, psi);
                    } else if (funcName.equals("push")) {
                        // require one T, return void
                        // requires pc => integrity of the array var
                        // require the element => integrity of the array var
                        Expression arg = args.get(0);
                        ExpOutcome argOutcome = arg.genConsVisit(env, false);
                        psi.join(argOutcome.psi);
                        String argLabel = argOutcome.valueLabelName;
                        Utils.contextFlow(env, psi.getNormalPath().c, endContext, location);
                        ifNamePc = Utils.getLabelNamePc(scopeContext.getSHErrLocName());
                        env.cons.add(
                                new Constraint(new Inequality(ifNamePc, var.ifl.toSHErrLocFmt()), env.hypothesis(),
                                        location, env.curContractSym().getName(),
                                        "Current control flow must be trusted to call this method"));
                        env.cons.add(
                                new Constraint(new Inequality(argLabel, var.ifl.toSHErrLocFmt()), env.hypothesis(),
                                        location, env.curContractSym().getName(),
                                        "Current control flow must be trusted to call this method"));
                        if (!tail_position) {
                            env.cons.add(new Constraint(
                                    new Inequality(psi.getNormalPath().c.lambda, beginContext.lambda),
                                    env.hypothesis(), location, env.curContractSym().getName(),
                                    typecheck.Utils.ERROR_MESSAGE_LOCK_IN_NONLAST_OPERATION));
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
                                    typecheck.Utils.ERROR_MESSAGE_LOCK_IN_NONLAST_OPERATION));
                        }
                        return new ExpOutcome(var.ifl.toSHErrLocFmt(), psi);
                    } else {
                        assert false: "type error";
                        return null;
                    }
                }

                externalTargetSym = var;
                namespace = var.toSHErrLocFmt();
                TypeSym conType = var.typeSym;
                externalContractSym = env.getContract(conType.getName());

                env.addSigReq(namespace, conType.getName());
                ifNamePc = Utils.getLabelNamePc(scopeContext.getSHErrLocName());
                funcSym = env.getContract(conType.getName()).getFunc(funcName);
                assert funcSym != null : "not found: " + conType.getName() + "." + funcName;

                dependentLabelMapping.put(funcSym.sender().toSHErrLocFmt(), env.thisSym().toSHErrLocFmt());

                ifFuncCallPcBefore = funcSym.externalPc();
                ifFuncCallPcAfter = funcSym.internalPc();
                ifFuncGammaLock = funcSym.callGamma();
            } else {
                assert false;
                return null;
            }
        } else {
            //a(b)
            funcName = ((Name) value).id;
            ifNamePc = Utils.getLabelNamePc(scopeContext.getSHErrLocName());
            if (!env.containsFunc(funcName)) {
                if (env.containsContract(funcName) || Utils.isPrimitiveType(funcName)) { //type cast
                    if (args.size() != 1) {
                        assert false;
                        return null;
                    }
                    String ifNameArgValue = argValueLabelNames.get(0);
                    typecheck.Utils.contextFlow(env, psi.getNormalPath().c, endContext,
                            args.get(0).location);
                    // env.outContext = endContext;
                    if (!tail_position) {
                        env.cons.add(new Constraint(
                                new Inequality(psi.getNormalPath().c.lambda, beginContext.lambda),
                                env.hypothesis(), location, env.curContractSym().getName(),
                                typecheck.Utils.ERROR_MESSAGE_LOCK_IN_NONLAST_OPERATION));
                    }
                    return new ExpOutcome(ifNameArgValue, psi);
                } else {
                    assert false: "method not found: " + funcName;
                    return null;
                }
            }
            funcSym = env.getFunc(funcName);

            dependentLabelMapping.put(funcSym.sender().toSHErrLocFmt(), env.sender().toSHErrLocFmt());

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

        // if external call and the target address is final, make this equal to the target address
        if (externalCall && externalTargetSym.isFinal) {
            dependentLabelMapping.put(
                    externalContractSym.thisSym().toSHErrLocFmt(),
                    externalTargetSym.toSHErrLocFmt());
        }

        for (int i = 0; i < args.size(); ++i) {
            Expression arg = args.get(i);
            VarSym argSym = funcSym.parameters.get(i);
            if (argSym.isPrincipalVar()) {

                if (arg instanceof Name) {
                    VarSym valueSym = (VarSym) env.getVar(((Name) arg).id);
                    if (valueSym.isPrincipalVar()) {
                        dependentLabelMapping.put(argSym.toSHErrLocFmt(), valueSym.toSHErrLocFmt());
                    }
                }
            }

            // env.prevContext = prevContext = tmp;
            String ifNameArgValue = argValueLabelNames.get(i);
            Label ifArgLabel = funcSym.getLabelArg(i);
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
            );
            env.cons.add(
                    new Constraint(
                            new Inequality(
                                    ifNamePc,
                                    Relation.LEQ,
                                    ifArgLabel.toSHErrLocFmt(dependentLabelMapping)),
                            env.hypothesis(), arg.location, env.curContractSym().getName(),
                            "Current control flow must be trusted to feed the " + Utils.ordNumString(i + 1)
                            + "-th argument value")
            );
        }

        if (externalCall) {
            env.cons.add(
                    new Constraint(
                            new Inequality(ifContRtn, ifFuncCallPcBefore.toSHErrLocFmt(dependentLabelMapping)),
                    env.hypothesis(), location, env.curContractSym().getName(),
                    "Target contract must be trusted to call this method"));
        }


        PathOutcome expPsi = new PathOutcome(new PsiUnit(new Context(
                Utils.joinLabels(psi.getNormalPath().c.pc, funcSym.endPc().toSHErrLocFmt(dependentLabelMapping)),
                Utils.joinLabels(funcSym.getLabelNameCallGamma(), funcSym.internalPcSLC())
        )));

        for (Map.Entry<ExceptionTypeSym, String> exp : funcSym.exceptions.entrySet()) {
            ExceptionTypeSym curSym = exp.getKey();
            String expLabelName = exp.getValue();
            expPsi.set(curSym, new PsiUnit(
                    new Context(
                            Utils.joinLabels(expLabelName, funcSym.externalPcSLC()),
                            Utils.joinLabels(ifFuncGammaLock.toSHErrLocFmt(dependentLabelMapping),
                                    ifFuncCallPcAfter.toSHErrLocFmt(dependentLabelMapping))),
                    true)); //TODO: dependent
            //PsiUnit psiUnit = env.psi.get(curSym);
            //env.cons.add(new Constraint(new Inequality(Utils.makeJoin(expLabelName, ifNameFuncCallPcAfter), psiUnit.pc), env.hypothesis, location, env.curContractSym.name,
            //"Exception " + curSym.name + " is not trusted enough to throw"));
        }

        //TODO

        typecheck.Utils.contextFlow(env, psi.getNormalPath().c, endContext, location);
        env.cons.add(
                new Constraint(new Inequality(ifNamePc, ifFuncCallPcBefore.toSHErrLocFmt(dependentLabelMapping)), env.hypothesis(),
                        location, env.curContractSym().getName(),
                        "Current control flow must be trusted to call this method"));
        env.cons.add(new Constraint(new Inequality(ifFuncCallPcBefore.toSHErrLocFmt(dependentLabelMapping),
                Utils.joinLabels(ifFuncCallPcAfter.toSHErrLocFmt(dependentLabelMapping), beginContext.lambda)), env.hypothesis(),
                location, env.curContractSym().getName(),
                "Calling this function does not respect static reentrancy locks"));

        env.cons.add(new Constraint(
                new Inequality(Utils.joinLabels(ifFuncCallPcAfter.toSHErrLocFmt(dependentLabelMapping), ifFuncGammaLock.toSHErrLocFmt(dependentLabelMapping)),
                        Relation.EQ, endContext.lambda), env.hypothesis(), location,
                env.curContractSym().getName(),
                typecheck.Utils.ERROR_MESSAGE_LOCK_IN_NONLAST_OPERATION));

        if (!tail_position) {
            env.cons.add(new Constraint(
                    new Inequality(psi.getNormalPath().c.lambda, beginContext.lambda),
                    env.hypothesis(), location, env.curContractSym().getName(),
                    typecheck.Utils.ERROR_MESSAGE_LOCK_IN_NONLAST_OPERATION));
        }

        String ifNameFuncRtnValue = funcSym.returnSLC();
        // String ifNameFuncRtnLock = funcSym.getLabelNameRtnLock();
        psi.joinExe(expPsi);

        return new ExpOutcome(ifNameFuncRtnValue, psi);
    }

    @Override
    public compile.ast.Expression solidityCodeGen(List<Statement> result, CompileEnv code) {
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
            if (funcSym.isPublic()) {
                funcName = Utils.methodNameHash(funcSym.plainSignature());
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
            callExp = new ExternalCall(target, funcName, argumentExps);
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
            SingleVar tempVar = new SingleVar(code.newTempVarName());
            result.add(new VarDec(compile.Utils.PRIMITIVE_TYPE_UINT, statVar.name()));
            result.add(new VarDec(compile.Utils.PRIMITIVE_TYPE_BYTES, dataVar.name()));
            result.add(new VarDec(new PrimitiveType(funcSym.returnType.getName()), dataVar.name()));
            result.add(new Assign(
                    List.of(statVar, dataVar),
                    callExp
            ));

            // map exceptionID
            int i = 1;
            for (Entry<ExceptionTypeSym, String> entry: funcSym.exceptions.entrySet()) {
                IfStatement ifexp = new IfStatement(
                        new BinaryExpression(compile.Utils.SOL_BOOL_EQUAL, statVar, new Literal(String.valueOf(i))),
                        List.of(new Assign(statVar, new Literal(String.valueOf(code.getExceptionId(entry.getKey())))))
                        );
                result.add(ifexp);
                ++i;
            }

            compile.ast.Expression condition = new BinaryExpression(compile.Utils.SOL_BOOL_NONEQUAL,
                    statVar, new Literal(compile.Utils.RETURNCODE_NORMAL));
            IfStatement test = new IfStatement(condition,
                    List.of(new Return(List.of(statVar, dataVar))));
            result.add(test);

            result.add(new Assign(tempVar,
                    code.decodeVars(
                            funcSym.parameters.stream().map(para -> new PrimitiveType(para.typeSym.getName())).collect(
                                    Collectors.toList()),
                            dataVar)));
            return tempVar;
        }
    }

//    public String toSolCode() {
//        logger.debug("toSOl: Call");
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
    public ArrayList<Node> children() {
        ArrayList<Node> rtn = new ArrayList<>();
        rtn.add(value);
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

    @Override
    public java.util.Map<String, compile.ast.Type> readMap(CompileEnv code) {
        Map<String, Type> result = new HashMap<>();
        for (Expression arg: args) {
            result.putAll(arg.readMap(code));
        }
        return result;
    }
}

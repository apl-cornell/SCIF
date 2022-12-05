package ast;

import compile.SolCode;
import typecheck.sherrlocUtils.Constraint;
import typecheck.sherrlocUtils.Inequality;
import typecheck.sherrlocUtils.Relation;
import typecheck.*;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

public class Call extends TrailerExpr {

    ArrayList<Expression> args;

    //TODO: starargs, kwargs
    public Call() {
        this.args = new ArrayList<>();
    }

    public Call(Expression x, ArrayList<Expression> ys) {
        value = x;
        args = ys;
    }

    public void addArg(Expression arg) {
        this.args.add(arg);
    }

    private void setArgs(ArrayList<Expression> args) {
        this.args = args;
    }

    public Expression getArgAt(int index) {
        return args.get(index);
    }

    public ScopeContext ntcGenCons(NTCEnv env, ScopeContext parent) {
        ScopeContext now = new ScopeContext(this, parent);
        String funcName;
        FuncSym funcSym;
        boolean extern = false;
        if (!(value instanceof Name)) {
            if (value instanceof Attribute) {
                // a.b(c), a must be a contract
                extern = true;
                Attribute att = (Attribute) value;
                String varName = ((Name) att.value).id;
                funcName = att.attr.id;
                Sym s = env.getCurSym(varName);
                logger.debug("var " + varName + ": " + s.name);
                if (!(s instanceof VarSym) || !(((VarSym) s).typeSym instanceof ContractSym)) {
                    System.err.println("a.b not found");
                    return null;
                }
                ContractSym contractSym = (ContractSym) ((VarSym) s).typeSym;
                s = contractSym.getFunc(funcName);
                if (s == null) {
                    System.err.println("func in a.b() not found");
                    return null;
                }

                funcSym = (FuncSym) s;
            } else {
                return null;
            }
        } else {
            // a(b)
            funcName = ((Name) value).id;
            Sym s = env.getCurSym(funcName);
            if (s == null) {
                System.err.println("func not found");
                return null;
            }
            if (!(s instanceof FuncSym)) {
                if (s instanceof ContractSym || s instanceof BuiltinTypeSym) {
                    env.addCons(now.genCons(s.name, Relation.EQ, env, location));
                    return now;
                }
                // err: type mismatch
                System.err.println("contract not found");
                return null;
            }
            funcSym = ((FuncSym) s);
        }
        // typecheck arguments
        for (int i = 0; i < args.size(); ++i) {
            Expression arg = args.get(i);
            TypeSym paraInfo = funcSym.parameters.get(i).typeSym;
            ScopeContext argContext = arg.ntcGenCons(env, now);
            String typeName = env.getSymName(paraInfo.name);
            env.addCons(argContext.genCons(typeName, Relation.GEQ, env, arg.location));
        }
        String rtnTypeName = funcSym.returnType.name;
        env.addCons(now.genCons(env.getSymName(rtnTypeName), Relation.EQ, env, location));

        for (HashMap.Entry<ExceptionTypeSym, String> tl : funcSym.exceptions.entrySet()) {
            if (!parent.isCheckedException(tl.getKey(), extern)) {
                System.err.println(
                        "Unchecked exception: " + tl.getKey().name + " at " + location.toString());
                throw new RuntimeException();
            }
        }
        return now;
    }

    @Override
    public ExpOutcome genConsVisit(VisitEnv env, boolean tail_position) {
        //TODO: Assuming value is a Name for now
        Context beginContext = env.inContext;
        Context endContext = new Context(typecheck.Utils.getLabelNamePc(location),
                typecheck.Utils.getLabelNameLock(location));

        ArrayList<String> argValueLabelNames = new ArrayList<>();

        PathOutcome psi = new PathOutcome(new PsiUnit(beginContext));
        ExpOutcome ao = null;

        for (int i = 0; i < args.size(); ++i) {
            Expression arg = args.get(i);
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
        String ifNameFuncCallPcBefore, ifNameFuncCallPcAfter, ifNameFuncGammaLock;

        ExpOutcome vo = null;

        if (!(value instanceof Name)) {
            if (value instanceof Attribute) {
                //  the case: a.b(c) where a is a contract, b is a function and c are the arguments
                // att = a.b
                logger.debug("call value: " + value.toSolCode());
                Attribute att = (Attribute) value;
                vo = att.value.genConsVisit(env, false);
                psi.joinExe(vo.psi);
                String ifContRtn = vo.valueLabelName;

                //TODO: assuming a's depth is 1
                String varName = ((Name) att.value).id;
                VarSym var = env.getVar(varName);
                namespace = var.toSherrlocFmt();
                TypeSym conType = var.typeSym;
                env.addSigReq(namespace, conType.name);
                funcName = (att.attr).id;
                ifNamePc = Utils.getLabelNamePc(scopeContext.getSHErrLocName());
                funcSym = env.getContract(conType.name).getFunc(funcName);
                if (funcSym instanceof PolyFuncSym) {
                    ((PolyFuncSym) funcSym).apply();
                }
                ifNameFuncCallPcBefore = funcSym.getLabelNameCallPcBefore(namespace);
                ifNameFuncCallPcAfter = funcSym.getLabelNameCallPcAfter(namespace);
                ifNameFuncGammaLock = funcSym.getLabelNameCallGamma(namespace);
                env.cons.add(new Constraint(new Inequality(ifContRtn, ifNameFuncCallPcBefore),
                        env.hypothesis, location, env.curContractSym.name,
                        "Argument value must be trusted to call this method"));
            } else {
                return null;
            }
        } else {
            //a(b)
            System.out.println("a(b)");
            funcName = ((Name) value).id;
            ifNamePc = Utils.getLabelNamePc(scopeContext.getSHErrLocName());
            if (!env.containsFunc(funcName)) {
                if (env.containsContract(funcName) || Utils.isPrimitiveType(funcName)) { //type cast
                    if (args.size() != 1) {
                        return null;
                    }
                    String ifNameArgValue = argValueLabelNames.get(0);
                    typecheck.Utils.contextFlow(env, psi.getNormalPath().c, endContext,
                            args.get(0).location);
                    // env.outContext = endContext;
                    if (!tail_position) {
                        env.cons.add(new Constraint(
                                new Inequality(psi.getNormalPath().c.lambda, beginContext.lambda),
                                env.hypothesis, location, env.curContractSym.name,
                                typecheck.Utils.ERROR_MESSAGE_LOCK_IN_NONLAST_OPERATION));
                    }
                    return new ExpOutcome(ifNameArgValue, psi);
                } else {
                    return null;
                }
            }
            funcSym = env.getFunc(funcName);
            if (funcSym instanceof PolyFuncSym) {
                ((PolyFuncSym) funcSym).apply();
            }

            ifNameFuncCallPcBefore = funcSym.getLabelNameCallPcBefore();
            ifNameFuncCallPcAfter = funcSym.getLabelNameCallPcAfter();
            ifNameFuncGammaLock = funcSym.getLabelNameCallGamma();
        }

        for (int i = 0; i < args.size(); ++i) {
            Expression arg = args.get(i);
            // env.prevContext = prevContext = tmp;
            String ifNameArgValue = argValueLabelNames.get(i);
            String ifNameArgLabel = funcSym.getLabelNameArg(i);
            env.cons.add(
                    new Constraint(new Inequality(ifNameArgValue, Relation.LEQ, ifNameArgLabel),
                            env.hypothesis, arg.location, env.curContractSym.name,
                            "Input to the " + Utils.ordNumString(i + 1)
                                    + " argument must be trusted enough"));
            env.cons.add(new Constraint(new Inequality(ifNamePc, Relation.LEQ, ifNameArgLabel),
                    env.hypothesis, arg.location, env.curContractSym.name,
                    "Current control flow must be trusted to feed the " + Utils.ordNumString(i + 1)
                            + "-th argument value"));
        }
        if (funcSym instanceof PolyFuncSym) {
        }

        PathOutcome expPsi = new PathOutcome(new PsiUnit(new Context(
                Utils.joinLabels(psi.getNormalPath().c.pc, funcSym.getLabelNameCallPcEnd()),
                Utils.joinLabels(funcSym.getLabelNameCallGamma(), funcSym.getLabelNameCallPcAfter())
        )));

        for (Map.Entry<ExceptionTypeSym, String> exp : funcSym.exceptions.entrySet()) {
            ExceptionTypeSym curSym = exp.getKey();
            String expLabelName = exp.getValue();
            expPsi.set(curSym, new PsiUnit(
                    new Context(
                            Utils.makeJoin(expLabelName, funcSym.getLabelNameCallPcBefore()),
                            Utils.makeJoin(funcSym.getLabelNameCallGamma(),
                                    funcSym.getLabelNameCallPcAfter())),
                    true));
            //PsiUnit psiUnit = env.psi.get(curSym);
            //env.cons.add(new Constraint(new Inequality(Utils.makeJoin(expLabelName, ifNameFuncCallPcAfter), psiUnit.pc), env.hypothesis, location, env.curContractSym.name,
            //"Exception " + curSym.name + " is not trusted enough to throw"));
        }

        //TODO
        env.cons.add(
                new Constraint(new Inequality(ifNamePc, ifNameFuncCallPcBefore), env.hypothesis,
                        location, env.curContractSym.name,
                        "Current control flow must be trusted to call this method"));
        env.cons.add(new Constraint(new Inequality(ifNameFuncCallPcBefore,
                Utils.makeJoin(ifNameFuncCallPcAfter, beginContext.lambda)), env.hypothesis,
                location, env.curContractSym.name,
                "Calling this function does not respect static reentrancy locks"));
        env.cons.add(new Constraint(
                new Inequality(Utils.makeJoin(ifNameFuncCallPcAfter, ifNameFuncGammaLock),
                        Relation.EQ, endContext.lambda), env.hypothesis, location,
                env.curContractSym.name,
                "Calling this function does not respect static reentrancy locks"));
        ;

        if (!tail_position) {
            env.cons.add(new Constraint(
                    new Inequality(psi.getNormalPath().c.lambda, beginContext.lambda),
                    env.hypothesis, location, env.curContractSym.name,
                    typecheck.Utils.ERROR_MESSAGE_LOCK_IN_NONLAST_OPERATION));
        }

        String ifNameFuncRtnValue = funcSym.getLabelNameRtnValue(namespace);
        // String ifNameFuncRtnLock = funcSym.getLabelNameRtnLock();
        psi.joinExe(expPsi);
        return new ExpOutcome(ifNameFuncRtnValue, psi);
    }

    public String toSolCode() {
        logger.debug("toSOl: Call");
        String funcName = value.toSolCode();
        if (Utils.isBuiltinFunc(funcName)) {
            return Utils.transBuiltinFunc(funcName, this);
        }
        String argsCode = "";
        boolean first = true;
        for (Expression exp : args) {
            if (!first) {
                argsCode += ", ";
            } else {
                first = false;
            }
            argsCode += exp.toSolCode();
        }

        return SolCode.toFunctionCall(funcName, argsCode);
    }

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
}

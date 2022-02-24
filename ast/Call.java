package ast;

import compile.SolCode;
import sherrlocUtils.Constraint;
import sherrlocUtils.Inequality;
import sherrlocUtils.Relation;
import typecheck.*;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;

public class Call extends TrailerExpr {
    public ArrayList<Expression> args;
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
    public void setArgs(ArrayList<Expression> args) { this.args = args; }

    public ScopeContext NTCgenCons(NTCEnv env, ScopeContext parent) {
        ScopeContext now = new ScopeContext(this, parent);
        String funcName;
        FuncSym funcSym;
        if (!(value instanceof Name)) {
            if (value instanceof Attribute) {
                // a.b(c), a must be a contract
                Attribute att = (Attribute) value;
                String varName = ((Name)att.value).id;
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
            ScopeContext argContext = arg.NTCgenCons(env, now);
            String typeName = env.getSymName(paraInfo.name);
            env.addCons(argContext.genCons(typeName, Relation.GEQ, env, location));
        }
        String rtnTypeName = funcSym.returnType.name;
        env.addCons(now.genCons(env.getSymName(rtnTypeName), Relation.EQ, env, location));

        for (ExceptionTypeSym tl : funcSym.exceptions) {
            if (!parent.isCheckedException(tl)) {
                System.err.println("Unchecked exception: " + tl.name + " at " + location.toString());
                throw new RuntimeException();
            }
        }
        return now;
    }

    @Override
    public Context genConsVisit(VisitEnv env, boolean tail_position) {
        //TODO: Assuming value is a Name for now

        Context context = env.context;
        Context curContext = new Context(context.valueLabelName, Utils.getLabelNameLock(location), context.inLockName);

        String funcName;
        String ifNamePc;
        FuncSym funcSym;
        String namespace = "";
        String ifNameFuncCallPcBefore, ifNameFuncCallPcAfter, ifNameFuncGammaLock;

        if (!(value instanceof Name)) {
            if (value instanceof Attribute) {
                //  the case: a.b(c) where a is a contract, b is a function and c are the arguments
                // att = a.b
                logger.debug("call value: " + value.toSolCode());
                Attribute att = (Attribute) value;
                Context tmp = att.value.genConsVisit(env, false);
                // env.prevContext = prevContext;
                String ifContRtn = tmp.valueLabelName; //the label of called contract

                //TODO: assuming a's depth is 1
                String varName = ((Name)att.value).id;
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
                env.cons.add(new Constraint(new Inequality(ifContRtn, ifNameFuncCallPcBefore), env.hypothesis, location, env.curContractSym.name,
                        "Argument value must be trusted to call this method"));
            } else {
                return null;
            }
        } else {
            funcName = ((Name) value).id;
            ifNamePc = Utils.getLabelNamePc(scopeContext.getSHErrLocName());
            if (!env.containsFunc(funcName)) {
                if (env.containsContract(funcName) || Utils.isPrimitiveType(funcName)) { //type cast
                    if (args.size() != 1) return null;
                    Context tmp = args.get(0).genConsVisit(env, false);
                    // env.prevContext = prevContext = tmp;
                    String ifNameArgValue = tmp.valueLabelName;
                    if (!tail_position) {
                        env.cons.add(new Constraint(new Inequality(curContext.lockName, context.inLockName), env.hypothesis, location, env.curContractSym.name,
                                typecheck.Utils.ERROR_MESSAGE_LOCK_IN_NONLAST_OPERATION));
                    } else {
                        env.cons.add(new Constraint(new Inequality(curContext.lockName, context.lockName), env.hypothesis, location, env.curContractSym.name,
                                typecheck.Utils.ERROR_MESSAGE_LOCK_IN_LAST_OPERATION));
                    }
                    return new Context(ifNameArgValue, curContext.lockName, curContext.inLockName);
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
            env.context = context;
            Context tmp = arg.genConsVisit(env, false);
            // env.prevContext = prevContext = tmp;
            String ifNameArgValue = tmp.valueLabelName;
            String ifNameArgLabel = funcSym.getLabelNameArg(i);
            env.cons.add(new Constraint(new Inequality(ifNameArgValue, Relation.LEQ, ifNameArgLabel), env.hypothesis, arg.location, env.curContractSym.name,
                    "Input to the " + Utils.ordNumString(i + 1) + " argument must be trusted enough"));

            env.cons.add(new Constraint(new Inequality(ifNamePc, Relation.LEQ, ifNameArgLabel), env.hypothesis, arg.location, env.curContractSym.name,
                    "Current control flow must be trusted to feed the " + Utils.ordNumString(i + 1) + "-th argument value"));

            // env.cons.add(new Constraint(new Inequality(prevLockName, Relation.LEQ, tmp.lockName), env.hypothesis, arg.location, env.curContractSym.name,                    Utils.ERROR_MESSAGE_LOCK_IN_NONLAST_OPERATION));

        }
        if (funcSym instanceof PolyFuncSym) {
        }
        env.cons.add(new Constraint(new Inequality(ifNamePc, ifNameFuncCallPcBefore), env.hypothesis, location, env.curContractSym.name,
                "Current control flow must be trusted to call this method"));
        env.cons.add(new Constraint(new Inequality(ifNameFuncCallPcBefore, Utils.makeJoin(ifNameFuncCallPcAfter, curContext.inLockName)), env.hypothesis, location, env.curContractSym.name,
                "Calling this function does not respect static reentrancy locks"));
        env.cons.add(new Constraint(new Inequality(Utils.makeJoin(ifNameFuncCallPcAfter, ifNameFuncGammaLock), curContext.lockName), env.hypothesis, location, env.curContractSym.name,
                "Calling this function does not respect static reentrancy locks"));;


        if (!tail_position) {
            env.cons.add(new Constraint(new Inequality(curContext.lockName, context.inLockName), env.hypothesis, location, env.curContractSym.name,
                    typecheck.Utils.ERROR_MESSAGE_LOCK_IN_NONLAST_OPERATION));
        } else {
            env.cons.add(new Constraint(new Inequality(curContext.lockName, context.lockName), env.hypothesis, location, env.curContractSym.name,
                    typecheck.Utils.ERROR_MESSAGE_LOCK_IN_LAST_OPERATION));
        }

        String ifNameFuncRtnValue = funcSym.getLabelNameRtnValue(namespace);
        // String ifNameFuncRtnLock = funcSym.getLabelNameRtnLock();
        return new Context(ifNameFuncRtnValue, curContext.lockName, curContext.inLockName);
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
            if (!first)
                argsCode += ", ";
            else
                first = false;
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
                super.typeMatch(expression)))
            return false;

        Call c = (Call) expression;

        boolean bothArgsNull = c.args == null && args == null;

        if (!bothArgsNull) {
            if (args == null || c.args == null || args.size() != c.args.size())
                return false;
            int index = 0;
            while (index < args.size()) {
                if (!args.get(index).typeMatch(c.args.get(index)))
                    return false;
                ++index;
            }
        }

        return true;
    }
}

package ast;

import compile.SolCode;
import sherrlocUtils.Constraint;
import sherrlocUtils.Inequality;
import sherrlocUtils.Relation;
import typecheck.*;

import javax.swing.text.Utilities;
import java.util.ArrayList;
import java.util.HashMap;

public class Call extends TrailerExpr {
    public ArrayList<Expression> args;
    ArrayList<Keyword> keywords;
    //TODO: starargs, kwargs
    public Call() {
        this.args = new ArrayList<>();
        this.keywords = new ArrayList<>();
    }
    public Call(Expression x, ArrayList<Expression> ys) {
        value = x;
        args = ys;
        keywords = null;
    }
    public Call(Expression x, ArrayList<Expression> ys, ArrayList<Keyword> zs) {
        value = x;
        args = ys;
        keywords = zs;
    }
    public void addArg(Expression arg) {
        this.args.add(arg);
    }
    public void addKeyword(Keyword keyword) {
        this.keywords.add(keyword);
    }

    public NTCContext NTCgenCons(NTCEnv env, NTCContext parent) {
        NTCContext now = new NTCContext(this, parent);
        String funcName;
        FuncInfo funcInfo;
        if (!(value instanceof Name)) {
            if (value instanceof Attribute) {
                // a.b(c), a must be a contract
                Attribute att = (Attribute) value;
                String contractName = ((Name)att.value).id;
                funcName = att.attr.id;
                Sym s = env.getExtSym(contractName, funcName);
                funcInfo = ((FuncSym) s).funcInfo;
            } else {
                return null;
            }
        } else {
            // a(b)
            funcName = ((Name) value).id;
            Sym s = env.getCurSym(funcName);
            if (s == null) {
                if (env.contractExists(funcName)) {
                    // return type is ATM
                    // TODO
                }
                return null;
            }
            if (!(s instanceof FuncSym)) {
                // err: type mismatch
                return null;
            }
            funcInfo = ((FuncSym) s).funcInfo;
        }
        // typecheck arguments
        for (int i = 0; i < args.size(); ++i) {
            Expression arg = args.get(i);
            TypeInfo paraInfo = funcInfo.parameters.get(i).typeInfo;
            NTCContext argContext = arg.NTCgenCons(env, now);
            String typeName = env.getSymName(paraInfo.type.typeName);
            env.addCons(argContext.genCons(typeName, Relation.REQ, env, location));
        }
        String rtnTypeName = funcInfo.returnType.type.typeName;
        env.addCons(now.genCons(env.getSymName(rtnTypeName), Relation.EQ, env, location));
        return now;
    }

    @Override
    public Context genConsVisit(VisitEnv env) {
        //TODO: Assuming value is a Name for now
        String funcName;
        String ifNamePc;
        FuncInfo funcInfo;
        String ifNameFuncCallPc, ifNameFuncCallLock;
        String prevLockName = env.prevContext.lockName;

        if (!(value instanceof Name)) {
            if (value instanceof Attribute) {
                //  the case: a.b(c) where a is a contract, b is a function and c are the arguments
                // att = a.b
                logger.debug("call value: " + value.toString());
                Attribute att = (Attribute) value;
                String ifContRtn = att.value.genConsVisit(env).valueLabelName; //the label of called contract
                //TODO: assuming a's depth is 1
                String varName = ((Name)att.value).id;
                TypeInfo conType = env.varNameMap.getInfo(varName).typeInfo;
                funcName = (att.attr).id;
                ifNamePc = Utils.getLabelNamePc(env.ctxt);
                funcInfo = env.contractMap.get(conType.type.typeName).funcMap.get(funcName);
                if (funcInfo instanceof PolyFuncInfo) {
                    ((PolyFuncInfo)funcInfo).apply();
                }
                ifNameFuncCallPc = funcInfo.getLabelNameCallPc();
                ifNameFuncCallLock = funcInfo.getLabelNameCallLock();
                env.cons.add(new Constraint(new Inequality(ifContRtn, ifNameFuncCallPc), env.hypothesis, location));
            } else {
                return null;
            }
        } else {
            funcName = ((Name) value).id;
            ifNamePc = Utils.getLabelNamePc(env.ctxt);
        /*if (funcName.equals(Utils.ENDORCEFUNCNAME)) {
            //TODO: didn't add explicit ifLabel expression parsing at this point
            String ifNameExp = args.get(0).genConsVisit(ctxt, funcMap, cons, varNameMap);
            String ifNameFrom = args.get(1).genConsVisit(ctxt, funcMap, cons, varNameMap);
            String ifNameTo = args.get(2).genConsVisit(ctxt, funcMap, cons, varNameMap);
            String ifNameRnt = ctxt + "." + "endorse" + location.toString();
            cons.add(Utils.genCons(ifNameExp, ifNameFrom, location));
            cons.add(Utils.genCons(ifNameFrom, ifNameExp, location));
            cons.add(Utils.genCons(ifNameRnt, ifNameTo, location));
            cons.add(Utils.genCons(ifNameTo, ifNameRnt, location));
            return ifNameRnt;
        }
        else*/
            if (!env.funcMap.containsKey(funcName)) {
                if (env.contractMap.containsKey(funcName)) { //init contract
                    if (args.size() != 1) return null;
                    Context tmp = args.get(0).genConsVisit(env);
                    String ifNameArgValue = tmp.valueLabelName;
                    return new Context(ifNameArgValue, tmp.lockName);
                } else {
                    return null;
                }
            }
            funcInfo = env.funcMap.get(funcName);
            if (funcInfo instanceof PolyFuncInfo) {
                ((PolyFuncInfo) funcInfo).apply();
            }
            ifNameFuncCallPc = funcInfo.getLabelNameCallPc();
            ifNameFuncCallLock = funcInfo.getLabelNameCallLock();
        }
            env.cons.add(new Constraint(new Inequality(ifNamePc, ifNameFuncCallPc), env.hypothesis, location));
            env.cons.add(new Constraint(new Inequality(ifNameFuncCallLock, prevLockName), env.hypothesis, location));


            //TODO: keywords style arg assign
            for (int i = 0; i < args.size(); ++i) {
                Expression arg = args.get(i);
                Context tmp = arg.genConsVisit(env);
                String ifNameArgValue = tmp.valueLabelName;
                String ifNameArgLabel = funcInfo.getLabelNameArg(i);
                env.cons.add(new Constraint(new Inequality(ifNameArgValue, Relation.LEQ, ifNameArgLabel), env.hypothesis, location));

                env.cons.add(new Constraint(new Inequality(ifNamePc, Relation.LEQ, ifNameArgLabel), env.hypothesis, location));

                env.cons.add(new Constraint(new Inequality(prevLockName, Relation.EQ, tmp.lockName), env.hypothesis, location));

            }
            if (funcInfo instanceof PolyFuncInfo) {
                // TODO: simplify
                String ifNameCallBeforeLabel = funcInfo.getLabelNameCallPc();
                String ifNameCallAfterLabel = funcInfo.getLabelNameCallPc();
                String ifNameCallLockLabel = funcInfo.getLabelNameCallLock();
                String ifCallBeforeLabel = funcInfo.getCallPcLabel();
                String ifCallAfterLabel = funcInfo.getCallPcLabel();
                String ifCallLockLabel = funcInfo.getCallLockLabel();

                if (ifCallBeforeLabel != null) {
                    env.cons.add(new Constraint(new Inequality(ifCallBeforeLabel, Relation.EQ, ifNameCallBeforeLabel), location));
                }
                if (ifCallAfterLabel != null) {
                    env.cons.add(new Constraint(new Inequality(ifCallAfterLabel, Relation.EQ, ifNameCallAfterLabel), location));
                }
                if (ifCallLockLabel != null) {
                    env.cons.add(new Constraint(new Inequality(ifCallLockLabel, Relation.EQ, ifNameCallLockLabel), location));
                }

                String ifNameRtnValueLabel = funcInfo.getLabelNameRtnValue();
                String ifRtnValueLabel = funcInfo.getRtnValueLabel();
                String ifNameRtnLockLabel = funcInfo.getLabelNameRtnLock();
                String ifRtnLockLabel = funcInfo.getRtnLockLabel();

                if (ifRtnValueLabel != null) {
                    env.cons.add(new Constraint(new Inequality(ifRtnValueLabel, Relation.EQ, ifNameRtnValueLabel), location));
                }
                if (ifRtnLockLabel != null) {
                    env.cons.add(new Constraint(new Inequality(ifRtnLockLabel, Relation.EQ, ifNameRtnLockLabel), location));
                }

                for (int i = 0; i < funcInfo.parameters.size(); ++i) {
                    VarInfo arg = funcInfo.parameters.get(i);
                    if (arg.typeInfo.ifl == null) continue;;
                    String ifNameArgLabel = funcInfo.getLabelNameArg(i);
                    String ifArgLabel = ((PolyFuncInfo) funcInfo).getArgLabel(i);
                    if (ifArgLabel != null) {
                        env.cons.add(new Constraint(new Inequality(ifNameArgLabel, Relation.LEQ, ifArgLabel), location));

                        //env.cons.add(new Constraint(new Inequality(ifArgLabel, ifNameArgLabel), arg.location));

                    }
                }
            }
            String ifNameFuncRtnValue = funcInfo.getLabelNameRtnValue();
            String ifNameFuncRtnLock = funcInfo.getLabelNameRtnLock();
            return new Context(ifNameFuncRtnValue, ifNameFuncRtnLock);
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

}

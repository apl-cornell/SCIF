package ast;

import compile.SolCode;
import sherrlocUtils.Constraint;
import sherrlocUtils.Inequality;
import sherrlocUtils.Relation;
import typecheck.*;

import java.util.ArrayList;

public class Return extends NonFirstLayerStatement {
    Expression value;
    public Return(Expression value) {
        this.value = value;
    }
    public Return() {
        this.value = null;
    }

    public ScopeContext NTCgenCons(NTCEnv env, ScopeContext parent) {
        ScopeContext now = new ScopeContext(this, parent);
        FuncSym funcSym = Utils.getCurrentFuncInfo(env, now);
        if (value != null) {
            ScopeContext rtn = value.NTCgenCons(env, now);
            env.addCons(now.genCons(rtn, Relation.EQ, env, location));
        }


        env.addCons(now.genCons(env.getSymName(funcSym.returnType.name), Relation.EQ, env, location));
        return now;
    }

    @Override
    public Context genConsVisit(VisitEnv env, boolean tail_position) {
        Context context = env.context;
        Context curContext = new Context(context.valueLabelName, Utils.getLabelNameLock(location), context.inLockName);
        // String prevLock = env.prevContext.lockName;
        String ifNamePc = Utils.getLabelNamePc(scopeContext.getSHErrLocName());

        env.context = curContext;
        // int occur = env.ctxt.indexOf(".");
        String funcName = scopeContext.getFuncName();
        FuncSym funcSym = env.getFunc(funcName);
        // String ifNameRtnLock = funcSym.getLabelNameRtnLock();
        String ifNameRtnValue = funcSym.getLabelNameRtnValue();
        String ifRtnLockName = funcSym.getLabelNameCallGamma();
        if (value == null) {
            env.cons.add(new Constraint(new Inequality(ifNamePc, ifNameRtnValue), env.hypothesis, location, env.curContractSym.name,
                    Utils.ERROR_MESSAGE_LOCK_IN_NONLAST_OPERATION));
            return new Context(null, context.lockName, context.inLockName);
        }

        Context expContext = value.genConsVisit(env, true);
        String ifNameValue = expContext.valueLabelName;
        env.cons.add(new Constraint(new Inequality(ifNameValue, ifNameRtnValue), env.hypothesis, location, env.curContractSym.name,
                "Value must be trusted to be returned"));
        env.cons.add(new Constraint(new Inequality(ifNamePc, ifNameRtnValue), env.hypothesis, location, env.curContractSym.name,
                "Control flow must be trusted to return"));

        env.cons.add(new Constraint(new Inequality(Utils.makeJoin(curContext.lockName, curContext.inLockName), ifRtnLockName), env.hypothesis, location, env.curContractSym.name,
                "Reentrancy locks must be respected to return"));
        return new Context(null, curContext.lockName, curContext.inLockName);
    }

    @Override
    public void SolCodeGen(SolCode code) {
        code.addReturn(value != null ? value.toSolCode() : "");
    }
    @Override
    public ArrayList<Node> children() {
        ArrayList<Node> rtn = new ArrayList<>();
        if (value != null)
            rtn.add(value);
        return rtn;
    }
}

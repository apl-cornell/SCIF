package ast;

import compile.SolCode;
import sherrlocUtils.Constraint;
import sherrlocUtils.Inequality;
import sherrlocUtils.Relation;
import typecheck.*;

import java.util.ArrayList;

public class Return extends Statement {
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
    public Context genConsVisit(VisitEnv env) {
        String prevLock = env.prevContext.lockName;
        String ifNamePc = Utils.getLabelNamePc(env.ctxt.getSHErrLocName());

        // int occur = env.ctxt.indexOf(".");
        String funcName = env.ctxt.getFuncName();
        FuncSym funcSym = env.getFunc(funcName);
        String ifNameRtnLock = funcSym.getLabelNameRtnLock();
        String ifNameRtnValue = funcSym.getLabelNameRtnValue();
        if (value == null) {
            env.cons.add(new Constraint(new Inequality(ifNamePc, ifNameRtnValue), env.hypothesis, location));
            return new Context(null, prevLock);
        }

        Context expContext = value.genConsVisit(env);
        String ifNameValue = expContext.valueLabelName;
        env.cons.add(new Constraint(new Inequality(ifNameValue, ifNameRtnValue), env.hypothesis, location));
        env.cons.add(new Constraint(new Inequality(ifNamePc, ifNameRtnValue), env.hypothesis, location));

        env.cons.add(new Constraint(new Inequality(expContext.lockName, ifNameRtnLock), env.hypothesis, location));
        return new Context(null, prevLock);
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

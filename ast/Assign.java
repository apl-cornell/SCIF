package ast;

import compile.SolCode;
import sherrlocUtils.Constraint;
import sherrlocUtils.Inequality;
import sherrlocUtils.Relation;
import typecheck.*;

import java.util.ArrayList;

public class Assign extends Statement {
    //TODO: assuming targets only contains 1 element now
    ArrayList<Expression> targets;
    Expression value;
    public Assign(ArrayList<Expression> targets, Expression value) {
        this.targets = targets;
        this.value = value;
    }

    @Override
    public ScopeContext NTCgenCons(NTCEnv env, ScopeContext parent) {
        ScopeContext now = new ScopeContext(this, parent);
        ScopeContext tgt = targets.get(0).NTCgenCons(env, now);
        ScopeContext v = value.NTCgenCons(env, now);
        // con: tgt should be a supertype of v
        env.cons.add(tgt.genCons(v, Relation.LEQ, env, location));
        return now;
    }

    @Override
    public Context genConsVisit(VisitEnv env) {
        String ifNamePc = Utils.getLabelNamePc(env.ctxt);
        String prevLockName = env.prevContext.lockName;
        String rtnLockName = "";
        Context valueContext = value.genConsVisit(env);
        String ifNameValue = valueContext.valueLabelName;

        String ifNameTgt = ""; //TODO: only support one argument now
        for (Expression target : targets) {
            if (target instanceof Name) {
                //Assuming target is Name
                ifNameTgt = env.varNameMap.getName(((Name) target).id) + "..lbl";
                rtnLockName = valueContext.lockName;
                /*VarInfo varInfo = env.varNameMap.getInfo(((Name) target).id);
                if (varInfo instanceof TestableVarInfo) {
                    ((TestableVarInfo) varInfo).tested = false;
                    ((TestableVarInfo) varInfo).testedLabel = Utils.DEAD;
                }*/
            } else if (target instanceof Subscript || target instanceof Attribute) {
                env.prevContext = valueContext;
                env.cons.add(new Constraint(new Inequality(prevLockName, CompareOperator.Eq, valueContext.lockName), env.hypothesis, location));
                Context tmp = target.genConsVisit(env);
                ifNameTgt = tmp.valueLabelName;
                rtnLockName = tmp.lockName;
            } else {
                //TODO: error handling
            }
            env.cons.add(new Constraint(new Inequality(ifNameValue, ifNameTgt), env.hypothesis, location));

            env.cons.add(new Constraint(new Inequality(ifNamePc, ifNameTgt), env.hypothesis, location));

            env.cons.add(new Constraint(new Inequality(prevLockName, CompareOperator.Eq, rtnLockName), env.hypothesis, location));

        }
        return new Context(ifNameTgt, rtnLockName);
    }

    public void SolCodeGen(SolCode code) {
        code.addAssign(targets.get(0).toSolCode(), value.toSolCode());
    }
    @Override
    public ArrayList<Node> children() {
        ArrayList<Node> rtn = new ArrayList<>();
        rtn.addAll(targets);
        rtn.add(value);
        return rtn;
    }
}

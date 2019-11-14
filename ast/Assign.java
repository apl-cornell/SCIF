package ast;

import sherrlocUtils.Constraint;
import sherrlocUtils.Inequality;
import typecheck.*;

import java.util.ArrayList;
import java.util.HashMap;

public class Assign extends Statement {
    ArrayList<Expression> targets;
    Expression value;
    public Assign(ArrayList<Expression> targets, Expression value) {
        this.targets = targets;
        this.value = value;
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
            } else if (target instanceof Subscript) {
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
}

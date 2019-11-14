package ast;

import sherrlocUtils.Constraint;
import sherrlocUtils.Inequality;
import sherrlocUtils.Relation;
import typecheck.Context;
import typecheck.Utils;
import typecheck.VisitEnv;

import java.util.ArrayList;

public class EndorseBlock extends Statement {
    IfLabel l_from, l_to;
    ArrayList<Statement> body;
    Expression target;

    public EndorseBlock(IfLabel l_from, IfLabel l_to, ArrayList<Statement> body, Expression target) {
        this.l_from = l_from;
        this.l_to = l_to;
        this.body = body;
        this.target = target;
    }

    public Context genConsVisit(VisitEnv env) {
        String originalCtxt = env.ctxt;
        String prevLockLabel = env.prevContext.lockName;

        String ifNamePcBefore = Utils.getLabelNamePc(env.ctxt);
        env.ctxt += ".endorseBlock" + location.toString();
        String ifNamePcAfter = Utils.getLabelNamePc(env.ctxt);

        String fromLabel = l_from.toSherrlocFmt();
        String toLabel = l_to.toSherrlocFmt();

        env.cons.add(new Constraint(new Inequality(ifNamePcBefore, fromLabel), env.hypothesis, location));

        env.cons.add(new Constraint(new Inequality(toLabel, Relation.EQ, ifNamePcAfter), env.hypothesis, location));
        env.cons.add(new Constraint(new Inequality(fromLabel, Utils.joinLabels(prevLockLabel, ifNamePcAfter)), env.hypothesis, location));
        String newLockLabel = Utils.getLabelNameLock(env.ctxt);
        env.cons.add(new Constraint(new Inequality(newLockLabel, Relation.EQ, Utils.meetLabels(prevLockLabel, ifNamePcAfter)), env.hypothesis, location));
        env.prevContext.lockName = newLockLabel;
        String newAfterLockLabel = Utils.getLabelNameLock(env.ctxt + ".after");

        Context lastContext = new Context(env.prevContext), prev2 = null;
        for (Statement stmt : body) {
            if (prev2 != null) {
                env.cons.add(new Constraint(new Inequality(lastContext.lockName, Relation.EQ, prev2.lockName), env.hypothesis, location));
            }
            Context tmp = stmt.genConsVisit(env);
            env.prevContext = tmp;
            prev2 = lastContext;
            lastContext = new Context(tmp);
        }
        env.cons.add(new Constraint(new Inequality(lastContext.lockName, newAfterLockLabel), env.hypothesis, location));
        env.prevContext.lockName = newAfterLockLabel;


        env.ctxt = originalCtxt;

        if (target == null) {
            return new Context(lastContext.valueLabelName, newAfterLockLabel);
        } else {
            String ifNameTgt = ""; //TODO: only support one argument now
            String rtnLockName = "";
            if (target instanceof Name) {
                //Assuming target is Name
                ifNameTgt = env.varNameMap.getName(((Name) target).id);
                rtnLockName = lastContext.lockName;
                /*VarInfo varInfo = env.varNameMap.getInfo(((Name) target).id);
                if (varInfo instanceof TestableVarInfo) {
                    ((TestableVarInfo) varInfo).tested = false;
                    ((TestableVarInfo) varInfo).testedLabel = Utils.DEAD;
                }*/
            } else if (target instanceof Subscript) {
                env.prevContext = lastContext;
                env.cons.add(new Constraint(new Inequality(prevLockLabel, CompareOperator.Eq, lastContext.lockName), env.hypothesis, location));
                Context tmp = target.genConsVisit(env);
                ifNameTgt = tmp.valueLabelName;
                rtnLockName = tmp.lockName;
            } else {
                //TODO: error handling
            }
            env.cons.add(new Constraint(new Inequality(lastContext.valueLabelName, ifNameTgt), env.hypothesis, location));

            env.cons.add(new Constraint(new Inequality(ifNamePcBefore, ifNameTgt), env.hypothesis, location));

            env.cons.add(new Constraint(new Inequality(prevLockLabel, CompareOperator.Eq, rtnLockName), env.hypothesis, location));
            return new Context(ifNameTgt, rtnLockName);
        }


    }
}

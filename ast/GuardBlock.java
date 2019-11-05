package ast;

import sherrlocUtils.Constraint;
import sherrlocUtils.Inequality;
import sherrlocUtils.Relation;
import typecheck.Context;
import typecheck.Utils;
import typecheck.VisitEnv;

import java.util.ArrayList;

public class GuardBlock extends Statement {
    IfLabel l;
    ArrayList<Statement> body;
    Expression target;

    public GuardBlock(IfLabel l, ArrayList<Statement> body, Expression target) {
        this.l = l;
        this.body = body;
        this.target = target;
    }

    public Context genConsVisit(VisitEnv env) {
        String originalCtxt = env.ctxt;
        String prevLockLabel = env.prevContext.lockName;

        String ifNamePc = Utils.getLabelNamePc(env.ctxt);
        env.ctxt += ".guardBlock" + location.toString();
        String newLockLabel = Utils.getLabelNameLock(env.ctxt);

        String guardLabel = l.toSherrlocFmt();

        env.cons.add(new Constraint(new Inequality(Utils.meetLabels(guardLabel, newLockLabel), prevLockLabel), env.hypothesis, location));
        String newAfterLockLabel = Utils.getLabelNameLock(env.ctxt + ".after");

        Context lastContext = env.prevContext;
        for (Statement s : body) {
            lastContext = s.genConsVisit(env);
            env.prevContext.lockName = lastContext.lockName;
        }
        env.cons.add(new Constraint(new Inequality(Utils.meetLabels(lastContext.lockName, guardLabel), newAfterLockLabel), env.hypothesis, location));
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

            env.cons.add(new Constraint(new Inequality(ifNamePc, ifNameTgt), env.hypothesis, location));

            env.cons.add(new Constraint(new Inequality(prevLockLabel, CompareOperator.Eq, rtnLockName), env.hypothesis, location));
            return new Context(ifNameTgt, rtnLockName);
        }


    }
}

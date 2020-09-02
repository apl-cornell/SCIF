package ast;

import compile.SolCode;
import sherrlocUtils.Constraint;
import sherrlocUtils.Inequality;
import sherrlocUtils.Relation;
import typecheck.*;

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

    public ScopeContext NTCgenCons(NTCEnv env, ScopeContext parent) {
        // consider to be a new scope
        // must contain at least one Statement
        env.curSymTab = new SymTab(env.curSymTab);
        ScopeContext rtn = null;
        for (Statement s : body) {
            rtn = s.NTCgenCons(env, parent);
        }
        env.curSymTab = env.curSymTab.getParent();
        return rtn;
    }

    public Context genConsVisit(VisitEnv env) {
        // String originalCtxt = env.ctxt;
        String prevLockLabel = env.prevContext.lockName;

        String ifNamePc = Utils.getLabelNamePc(scopeContext.getParent().getSHErrLocName());
        // env.ctxt += ".guardBlock" + location.toString();
        String newLockLabel = Utils.getLabelNameLock(scopeContext.getSHErrLocName());
        env.prevContext.lockName = newLockLabel;

        String guardLabel = l.toSherrlocFmt();

        env.cons.add(new Constraint(new Inequality(Utils.meetLabels(guardLabel, newLockLabel), prevLockLabel), env.hypothesis, location));
        String newAfterLockLabel = Utils.getLabelNameLock(scopeContext.getSHErrLocName() + ".after");

        Context lastContext = new Context(env.prevContext), prev2 = null;
        for (Statement stmt : body) {
            if (prev2 != null) {
                env.cons.add(new Constraint(new Inequality(lastContext.lockName, Relation.LEQ, prev2.lockName), env.hypothesis, location));
            }
            Context tmp = stmt.genConsVisit(env);
            env.prevContext = tmp;
            prev2 = lastContext;
            lastContext = new Context(tmp);
        }
        env.cons.add(new Constraint(new Inequality(Utils.meetLabels(lastContext.lockName, guardLabel), newAfterLockLabel), env.hypothesis, location));
        env.prevContext.lockName = newAfterLockLabel;


        // env.ctxt = originalCtxt;

        if (target == null) {
            return new Context(lastContext.valueLabelName, newAfterLockLabel);
        } else {
            String ifNameTgt = ""; //TODO: only support one argument now
            String rtnLockName = "";
            if (target instanceof Name) {
                //Assuming target is Name
                ifNameTgt = env.getVar(((Name) target).id).toSherrlocFmt();
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

    @Override
    public void SolCodeGen(SolCode code) {
        /*
            guard{l}
            lock(l)
         */
        code.enterGuard(l);
        for (Statement stmt : body) {
            stmt.SolCodeGen(code);
        }
        /*
            unlock(l);
         */
        code.exitGuard();
    }
    @Override
    public void passScopeContext(ScopeContext parent) {
        scopeContext = new ScopeContext(this, parent);
        for (Node node : children())
            node.passScopeContext(scopeContext);
    }
    @Override
    public ArrayList<Node> children() {
        ArrayList<Node> rtn = new ArrayList<>();
        rtn.add(l);
        rtn.addAll(body);
        if (target != null) rtn.add(target);
        return rtn;
    }
}

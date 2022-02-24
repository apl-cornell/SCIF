package ast;

import compile.SolCode;
import sherrlocUtils.Constraint;
import sherrlocUtils.Inequality;
import typecheck.*;

import java.util.ArrayList;

public class GuardBlock extends NonFirstLayerStatement {
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
        ScopeContext now  = new ScopeContext(this, parent);
        env.curSymTab = new SymTab(env.curSymTab);
        ScopeContext rtn = null;
        for (Statement s : body) {
            rtn = s.NTCgenCons(env, now);
            now.mergeExceptions(rtn);
        }
        env.curSymTab = env.curSymTab.getParent();
        return now;
    }

    public Context genConsVisit(VisitEnv env, boolean tail_position) {
        // String originalCtxt = env.ctxt;
        // String prevLockLabel = env.prevContext.lockName;
        Context context = env.context;
        Context curContext = new Context(context.valueLabelName, Utils.getLabelNameLock(location), Utils.getLabelNameInLock(location));
        // Context prevContext = env.prevContext;

        String ifNamePc = Utils.getLabelNamePc(scopeContext.getParent().getSHErrLocName());
        // env.ctxt += ".guardBlock" + location.toString();
        /*Context lockedContext = new Context(prevContext);
        lockedContext.lockName = lockName;
        lockedContext.inLockName = newInLock;*/

        // env.prevContext.lockName = newLockLabel;

        String guardLabel = l.toSherrlocFmt();

        env.cons.add(new Constraint(new Inequality(Utils.meetLabels(guardLabel, curContext.inLockName), context.inLockName), env.hypothesis, location, env.curContractSym.name,
                "Cannot put a dynamic reentrancy lock"));
        // String newAfterLockLabel = Utils.getLabelNameLock(scopeContext.getSHErrLocName() + ".after");

        Context lastContext = new Context(curContext);//, prev2 = null;
        int index = 0;
        for (Statement stmt : body) {
            ++index;
            /*if (prev2 != null) {
                env.cons.add(new Constraint(new Inequality(lastContext.lockName, Relation.LEQ, prev2.lockName), env.hypothesis, location, env.curContractSym.name,
                        Utils.ERROR_MESSAGE_LOCK_IN_NONLAST_OPERATION));
            }*/
            env.context = lastContext;
            Context tmp = stmt.genConsVisit(env, index == body.size() && tail_position);
            // env.prevContext = tmp;
            // prev2 = lastContext;
            // lastContext = tmp;
        }
        if (!tail_position) {
            env.cons.add(new Constraint(new Inequality(curContext.lockName, context.inLockName), env.hypothesis, location, env.curContractSym.name,
                    typecheck.Utils.ERROR_MESSAGE_LOCK_IN_NONLAST_OPERATION));
        } else {
            env.cons.add(new Constraint(new Inequality(curContext.lockName, context.lockName), env.hypothesis, location, env.curContractSym.name,
                    typecheck.Utils.ERROR_MESSAGE_LOCK_IN_LAST_OPERATION));
        }
        env.cons.add(new Constraint(new Inequality(Utils.meetLabels(curContext.lockName, guardLabel), context.lockName), env.hypothesis, location, env.curContractSym.name,
                "Cannot release a dynamic reentrancy lock"));
        // env.prevContext.lockName = newAfterLockLabel;

        // env.prevContext = prevContext;
        // env.ctxt = originalCtxt;

        //if (target == null) {
        return new Context(lastContext.valueLabelName, curContext.lockName, curContext.inLockName);
        /*} else {
            String ifNameTgt = ""; //TODO: only support one argument now
            String rtnLockName = "";
            if (target instanceof Name) {
                //Assuming target is Name
                ifNameTgt = env.getVar(((Name) target).id).toSherrlocFmt();
                rtnLockName = lastContext.lockName;
            } else if (target instanceof Subscript) {
                env.prevContext = lastContext;
                env.cons.add(new Constraint(new Inequality(prevLockLabel, CompareOperator.Eq, lastContext.lockName), env.hypothesis, location, env.curContractSym.name,
                        Utils.ERROR_MESSAGE_LOCK_IN_NONLAST_OPERATION));
                Context tmp = target.genConsVisit(env);
                ifNameTgt = tmp.valueLabelName;
                rtnLockName = tmp.lockName;
            } else {
                //TODO: error handling
            }
            env.cons.add(new Constraint(new Inequality(lastContext.valueLabelName, ifNameTgt), env.hypothesis, location, env.curContractSym.name,
                    "Value must be trusted enough for this assignment"));

            env.cons.add(new Constraint(new Inequality(ifNamePc, ifNameTgt), env.hypothesis, location, env.curContractSym.name,
                    "Control flow must allow this assignment"));

            env.cons.add(new Constraint(new Inequality(prevLockLabel, CompareOperator.Eq, rtnLockName), env.hypothesis, location, env.curContractSym.name,
                    Utils.ERROR_MESSAGE_LOCK_IN_NONLAST_OPERATION));
            return new Context(ifNameTgt, rtnLockName);
        }*/

    }

    @Override
    public void SolCodeGen(SolCode code) {
        /*
            guard{l}
            lock(l)
         */
        code.enterGuard(l);
        for (Statement stmt : body) {
            if (stmt instanceof Expression) {
                ((Expression) stmt).SolCodeGenStmt(code);
            } else {
                stmt.SolCodeGen(code);
            }
        }
        /*
            unlock(l);
         */
        code.exitGuard(l);
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

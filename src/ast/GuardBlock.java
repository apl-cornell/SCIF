package ast;

import compile.CompileEnv;
import compile.ast.Assert;
import compile.ast.Assign;
import compile.ast.Call;
import compile.ast.SingleVar;
import compile.ast.Type;
import java.util.List;
import java.util.Map;
import typecheck.sherrlocUtils.Constraint;
import typecheck.sherrlocUtils.Inequality;
import typecheck.*;

import java.util.ArrayList;
import java.util.HashMap;

public class GuardBlock extends Statement {

    PrimitiveIfLabel l;
    List<Statement> body;
    Expression target;

    public GuardBlock(PrimitiveIfLabel l, List<Statement> body, Expression target) {
        this.l = l;
        this.body = body;
        this.target = target;
    }

    public ScopeContext ntcGenCons(NTCEnv env, ScopeContext parent) {
        // consider to be a new scope
        // must contain at least one Statement
        ScopeContext now = new ScopeContext(this, parent);

        env.enterNewScope();
        ScopeContext rtn = null;
        for (Statement s : body) {
            rtn = s.ntcGenCons(env, now);
        }
        env.exitNewScope();

        return now;
    }

    public PathOutcome genConsVisit(VisitEnv env, boolean tail_position) {
        Context beginContext = env.inContext;
        Context curContext = new Context(beginContext.pc,
                scopeContext.getSHErrLocName() + "." + "lockin" + location.toString());
        Context endContext = new Context(typecheck.Utils.getLabelNamePc(toSHErrLocFmt()),
                typecheck.Utils.getLabelNameLock(toSHErrLocFmt()));

        String ifNamePc = Utils.getLabelNamePc(scopeContext.getParent().getSHErrLocName());
        // env.ctxt += ".guardBlock" + location.toString();
        /*Context lockedContext = new Context(prevContext);
        lockedContext.lambda = lambda;
        lockedContext.inLockName = newInLock;*/

        // env.prevContext.lambda = newLockLabel;

        Label label = env.curContractSym().newLabel(l);
        String guardLabel = label.toSHErrLocFmt();

        env.cons.add(new Constraint(new Inequality(Utils.meetLabels(guardLabel, curContext.lambda),
                beginContext.lambda), env.hypothesis(), location, env.curContractSym().getName(),
                "Cannot grant a dynamic reentrancy lock"));
        // String newAfterLockLabel = Utils.getLabelNameLock(scopeContext.getSHErrLocName() + ".after");

        PathOutcome psi = new PathOutcome(new PsiUnit(curContext));

        //Context lastContext = new Context(curContext);//, prev2 = null;
        env.incScopeLayer();
        PathOutcome so = null;
        int index = 0;
        for (Statement stmt : body) {
            ++index;
            /*if (prev2 != null) {
                env.cons.add(new Constraint(new Inequality(lastContext.lambda, Relation.LEQ, prev2.lambda), env.hypothesis, location, env.curContractSym.name,
                        Utils.ERROR_MESSAGE_LOCK_IN_NONLAST_OPERATION));
            }*/
            env.inContext = psi.getNormalPath().c;
            so = stmt.genConsVisit(env, index == body.size() && tail_position);
            psi.joinExe(so);
            // env.prevContext = tmp;
            // prev2 = lastContext;
            // lastContext = tmp;
        }
        env.decScopeLayer();

        PathOutcome psiOutcome = new PathOutcome();

        for (HashMap.Entry<ExceptionTypeSym, PsiUnit> entry : psi.psi.entrySet()) {
            PsiUnit value = entry.getValue();
            String newPathLabel =
                    scopeContext.getSHErrLocName() + "." + "lock" + location.toString() + "."
                            + entry.getKey().getName();
            env.cons.add(new Constraint(new Inequality(newPathLabel, CompareOperator.Eq,
                    Utils.meetLabels(value.c.lambda, guardLabel)), env.hypothesis(), location,
                    env.curContractSym().getName(),
                    "Operations inside lock clause does't respect locks"));
            psiOutcome.set(entry.getKey(),
                    new PsiUnit(new Context(value.c.pc, newPathLabel), value.catchable));
        }

        if (!tail_position) {
            env.cons.add(new Constraint(
                    new Inequality(psiOutcome.getNormalPath().c.lambda, beginContext.lambda),
                    env.hypothesis(), location, env.curContractSym().getName(),
                    typecheck.Utils.ERROR_MESSAGE_LOCK_IN_NONLAST_OPERATION));
        }
        /*env.cons.add(new Constraint(new Inequality(Utils.meetLabels(curContext.lockName, guardLabel), context.lockName), env.hypothesis, location, env.curContractSym.name,
                "Cannot release a dynamic reentrancy lock"));*/
        // env.prevContext.lambda = newAfterLockLabel;

        // env.prevContext = prevContext;
        // env.ctxt = originalCtxt;

        //if (target == null) {
        return psiOutcome;
        //new Context(lastContext.valueLabelName, curContext.lockName, curContext.inLockName);
        /*} else {
            String ifNameTgt = ""; //TODO: only support one argument now
            String rtnLockName = "";
            if (target instanceof Name) {
                //Assuming target is Name
                ifNameTgt = env.getVar(((Name) target).id).toSHErrLocFmt();
                rtnLockName = lastContext.lambda;
            } else if (target instanceof Subscript) {
                env.prevContext = lastContext;
                env.cons.add(new Constraint(new Inequality(prevLockLabel, CompareOperator.Eq, lastContext.lambda), env.hypothesis, location, env.curContractSym.name,
                        Utils.ERROR_MESSAGE_LOCK_IN_NONLAST_OPERATION));
                Context tmp = target.genConsVisit(env);
                ifNameTgt = tmp.valueLabelName;
                rtnLockName = tmp.lambda;
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
    public List<compile.ast.Statement> solidityCodeGen(CompileEnv code) {
        List<compile.ast.Statement> result = new ArrayList<>();
        code.enterNewVarScope();
        String lockP = l.value().id;
        result.add(new Assert(code.lock(lockP)));
        for (Statement stmt : body) {
            result.addAll(stmt.solidityCodeGen(code));
        }
        result.add(new Assert(code.unlock(lockP)));
        code.exitVarScope();
        return result;
    }

    @Override
    public void passScopeContext(ScopeContext parent) {
        scopeContext = new ScopeContext(this, parent);
        for (Node node : children()) {
            node.passScopeContext(scopeContext);
        }
    }

    @Override
    public ArrayList<Node> children() {
        ArrayList<Node> rtn = new ArrayList<>();
        rtn.add(l);
        rtn.addAll(body);
        if (target != null) {
            rtn.add(target);
        }
        return rtn;
    }
    @Override
    public boolean exceptionHandlingFree() {
        for (Statement s: body) {
            if (!s.exceptionHandlingFree()) {
                return false;
            }
        }
        return true;
    }

    @Override
    protected java.util.Map<String,? extends Type> readMap(CompileEnv code) {
        java.util.Map<String, Type> result = new HashMap<>();
        for (Statement s: body) {
            result.putAll(s.readMap(code));
        }
        return result;
    }

    @Override
    protected java.util.Map<String,? extends Type> writeMap(CompileEnv code) {
        Map<String, Type> result = new HashMap<>();
        for (Statement s: body) {
            result.putAll(s.writeMap(code));
        }
        return result;
    }
}

package ast;

import compile.CompileEnv;
import compile.ast.Assert;
import compile.ast.Type;
import java.util.List;
import java.util.Map;

import typecheck.exceptions.SemanticException;
import typecheck.*;

import java.util.ArrayList;
import java.util.HashMap;

public class GuardBlock extends Statement {

    PrimitiveIfLabel l;
    Block body;
    Expression target;

    public GuardBlock(PrimitiveIfLabel l, List<Statement> body, Expression target) {
        this.l = l;
        this.body = new Block(body);
        this.target = target;
    }

    public ScopeContext genTypeConstraints(NTCEnv env, ScopeContext parent) throws SemanticException {
        // consider to be a new scope
        // must contain at least one Statement
        ScopeContext now = new ScopeContext(this, parent);

        env.enterNewScope();
        body.genTypeConstraints(env, now);
        env.exitNewScope();

        return now;
    }

    public PathOutcome IFCVisit(VisitEnv env, boolean tail_position) throws SemanticException {
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

        Label label = env.toLabel(l);
        String guardLabel = label.toSHErrLocFmt();

        env.checkFlow(Utils.meetLabels(guardLabel, curContext.lambda), beginContext.lambda,
                location, "The lock(...) block must maintain the reentrancy lock held before it");
        // String newAfterLockLabel = Utils.getLabelNameLock(scopeContext.getSHErrLocName() + ".after");

        //Context lastContext = new Context(curContext);//, prev2 = null;
        env.incScopeLayer();
        env.inContext = curContext;
        PathOutcome psi = body.IFCVisit(env, tail_position);
//        int index = 0;
//        for (Statement stmt : body) {
//            ++index;
//            /*if (prev2 != null) {
//                env.cons.add(new Constraint(new Inequality(lastContext.lambda, Relation.LEQ, prev2.lambda), env.hypothesis, location, env.curContractSym.name,
//                        Utils.ERROR_MESSAGE_LOCK_IN_NONLAST_OPERATION));
//            }*/
//            env.inContext = psi.getNormalPath().c;
//            so = stmt.genConsVisit(env, index == body.size() && tail_position);
//            psi.joinExe(so);
//            // env.prevContext = tmp;
//            // prev2 = lastContext;
//            // lastContext = tmp;
//        }
        env.decScopeLayer();

        PathOutcome psiOutcome = new PathOutcome();

        for (HashMap.Entry<ExceptionTypeSym, PsiUnit> entry : psi.psi.entrySet()) {
            PsiUnit value = entry.getValue();
            String newPathLabel =
                    scopeContext.getSHErrLocName() + "." + "lock" + location.toString() + "."
                            + entry.getKey().getName();
            env.checkEqual(newPathLabel, Utils.meetLabels(value.c.lambda, guardLabel), location,
                    "The reentrancy lock leaving the lock(...) block combines the body's lock with the dynamic lock");
            psiOutcome.set(entry.getKey(),
                    new PsiUnit(new Context(value.c.pc, newPathLabel), value.catchable));
        }

        if (!tail_position) {
            env.checkFlow(psiOutcome.getNormalPath().c.lambda, beginContext.lambda, location,
                    typecheck.Utils.ERROR_MESSAGE_LOCK_IN_NONLAST_OPERATION);
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
        result.addAll(body.solidityCodeGen(code));
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
        rtn.add(body);
        if (target != null) {
            rtn.add(target);
        }
        return rtn;
    }
    @Override
    public boolean exceptionHandlingFree() {
        return body.exceptionHandlingFree();
    }

    @Override
    protected java.util.Map<String,? extends Type> readMap(CompileEnv code) {
        return body.readMap(code);
    }

    @Override
    protected java.util.Map<String,? extends Type> writeMap(CompileEnv code) {
        return body.writeMap(code);
    }
}

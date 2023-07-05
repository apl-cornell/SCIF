package ast;

import compile.SolCode;
import java.util.List;
import typecheck.sherrlocUtils.Constraint;
import typecheck.sherrlocUtils.Inequality;
import typecheck.*;

import java.util.ArrayList;

public class Try extends Statement {

    List<Statement> body;
    List<ExceptHandler> handlers;

    // ArrayList<Statement> orelse;
    // ArrayList<Statement> finalbody;
    public Try(List<Statement> body,
            List<ExceptHandler> handlers) {//, ArrayList<Statement> orelse, ArrayList<Statement> finalbody) {
        this.body = body;
        this.handlers = handlers;
        // this.orelse = orelse;
        // this.finalbody = finalbody;
    }


    public ScopeContext ntcGenCons(NTCEnv env, ScopeContext parent) {
        // consider to be a new scope
        // must contain at least one Statement
        ScopeContext now = new ScopeContext(this, parent);
        env.enterNewScope();
        ScopeContext rtn = null;

        // ScopeContext tryclause = new ScopeContext(this, now);
        ScopeContext tmp;

        //TODO: inc scope layer
        for (ExceptHandler h : handlers) {
            ExceptionTypeSym t = env.getExceptionTypeSym(h.type());
            assert t != null;
            now.addException(t, true);
        }

        for (Statement s : body) {
            tmp = s.ntcGenCons(env, now);
        }
        env.exitNewScope();

        for (ExceptHandler h : handlers) {
            tmp = h.ntcGenCons(env, parent);
        }
        return now;
    }

    @Override
    public void solidityCodeGen(SolCode code) {
        assert false;
    }

    @Override
    public PathOutcome genConsVisit(VisitEnv env, boolean tail_position) {
        Context beginContext = env.inContext;
        Context endContext = new Context(typecheck.Utils.getLabelNamePc(toSHErrLocFmt()),
                typecheck.Utils.getLabelNameLock(toSHErrLocFmt()));

        // add new exceptions to psi
        /*HashMap<ExceptionTypeSym, PsiUnit> oldPsi = env.psi;
        HashMap<ExceptionTypeSym, PsiUnit> newPsi = new HashMap<>();
        for (HashMap.Entry<ExceptionTypeSym, PsiUnit> p : oldPsi.entrySet()) {
            newPsi.put(p.getKey(), new PsiUnit(p.getValue().pc, p.getValue().lambda, p.getValue().inTx));
        }*/
        /*boolean inTx = this instanceof Extry;
        for (ExceptHandler h : handlers) {
            newPsi.put(env.toExceptionTypeSym(h.type), new PsiUnit(h.getHandlerPcLabelName(), h.getHandlerLockLabelName(), inTx));
            env.cons.add(new Constraint(new Inequality(h.getHandlerLockLabelName(), beginContext.lambda), env.hypothesis, location, env.curContractSym.name,
                    "Try clause should maintain locks when throwing exception " + h.name));
        }*/

        //env.psi = newPsi;
        PathOutcome psi = new PathOutcome();
        PathOutcome input = new PathOutcome();
        env.incScopeLayer();
        PathOutcome so = new PathOutcome(new PsiUnit(beginContext));
        for (Statement s : body) {
            so = s.genConsVisit(env, false);
            psi.joinExe(so);
            // env.inContext = new Context(so.getNormalPath().c.pc, beginContext.lambda);
            env.inContext = new Context(so.getNormalPath().c);

        }
        env.decScopeLayer();
        for (ExceptHandler h : handlers) {
            ExceptionTypeSym expSym = env.getExp(h.name());
            PsiUnit u = psi.psi.get(expSym);

            if (u != null) {
                env.cons.add(
                        new Constraint(new Inequality(u.c.lambda, beginContext.lambda), env.hypothesis(),
                                location, env.curContractSym().getName(),
                                "Try clause should maintain locks when throwing exception " + h.name()));
            /*env.cons.add(new Constraint(new Inequality(u.c.pc, h.), env.hypothesis, location, env.curContractSym.name,
                    "Try clause should maintain locks when throwing exception " + h.name));*/

                input.set(expSym, u);
                // psi.set(expSym, (Context) null);
                psi.remove(expSym);
            }
        }
        //Context cTry = env.outContext;

        //env.psi = oldPsi;
        for (ExceptHandler h : handlers) {
            env.incScopeLayer();
            //env.inContext = new Context(h.getHandlerPcLabelName(), beginContext.lambda);
            ExceptionTypeSym expSym = env.getExp(h.name());
            PsiUnit expUnit = input.psi.get(expSym);
            if (expUnit != null) {
                env.inContext = new Context(expUnit.c.pc, beginContext.lambda);
                PathOutcome ho = h.genConsVisit(env, tail_position);
                psi.join(ho);
            }
            env.decScopeLayer();
            // cTry = new Context(Utils.makeJoin(cTry.pc, env.outContext.outPcName), Utils.makeJoin(cTry.lambda, env.outContext.lockName));
        }

        typecheck.Utils.contextFlow(env, psi.getNormalPath().c, endContext, location);

        /*Utils.contextFlow(env, cTry, endContext, location);
        env.outContext = endContext;*/
        if (!tail_position) {
            env.cons.add(new Constraint(
                    new Inequality(endContext.lambda, beginContext.lambda),
                    env.hypothesis(), location, env.curContractSym().getName(),
                    typecheck.Utils.ERROR_MESSAGE_LOCK_IN_NONLAST_OPERATION));
        }

        return psi;
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
        rtn.addAll(body);
        rtn.addAll(handlers);
        return rtn;
    }
}

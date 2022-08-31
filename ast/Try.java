package ast;

import typecheck.sherrlocUtils.Constraint;
import typecheck.sherrlocUtils.Inequality;
import typecheck.*;

import java.util.ArrayList;

public class Try extends FirstLayerStatement {
    ArrayList<Statement> body;
    ArrayList<ExceptHandler> handlers;
    // ArrayList<Statement> orelse;
    // ArrayList<Statement> finalbody;
    public Try(ArrayList<Statement> body, ArrayList<ExceptHandler> handlers) {//, ArrayList<Statement> orelse, ArrayList<Statement> finalbody) {
        this.body = body;
        this.handlers = handlers;
        // this.orelse = orelse;
        // this.finalbody = finalbody;
    }


    public ScopeContext NTCgenCons(NTCEnv env, ScopeContext parent) {
        // consider to be a new scope
        // must contain at least one Statement
        ScopeContext now = new ScopeContext(this, parent);
        env.curSymTab = new SymTab(env.curSymTab);
        ScopeContext rtn = null;

        // ScopeContext tryclause = new ScopeContext(this, now);
        ScopeContext tmp;

        //TODO: inc scope layer
        for (ExceptHandler h : handlers) {
            now.addException(env.toExceptionTypeSym(h.type), false);
        }

        for (Statement s : body) {
            tmp = s.NTCgenCons(env, now);
        }
        env.curSymTab = env.curSymTab.getParent();

        for (ExceptHandler h : handlers) {
            tmp = h.NTCgenCons(env, parent);
        }
        return now;
    }

    @Override
    public void globalInfoVisit(ContractSym contractSym) {

    }

    @Override
    public PathOutcome genConsVisit(VisitEnv env, boolean tail_position) {
        Context beginContext = env.inContext;
        Context endContext = new Context(typecheck.Utils.getLabelNamePc(location), typecheck.Utils.getLabelNameLock(location));

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
            env.inContext = new Context(so.getNormalPath().c.pc, beginContext.lambda);
        }
        env.decScopeLayer();
        for (ExceptHandler h : handlers) {
            ExceptionTypeSym expSym = env.getExp(h.name);
            PsiUnit u = psi.psi.get(expSym);

            env.cons.add(new Constraint(new Inequality(u.c.lambda, beginContext.lambda), env.hypothesis, location, env.curContractSym.name,
                    "Try clause should maintain locks when throwing exception " + h.name));
            /*env.cons.add(new Constraint(new Inequality(u.c.pc, h.), env.hypothesis, location, env.curContractSym.name,
                    "Try clause should maintain locks when throwing exception " + h.name));*/

            input.set(expSym, u);
            psi.set(expSym, (Context) null);
        }
        //Context cTry = env.outContext;

        //env.psi = oldPsi;
        for (ExceptHandler h : handlers) {
            //TODO: inc layer
            //env.inContext = new Context(h.getHandlerPcLabelName(), beginContext.lambda);
            ExceptionTypeSym expSym = env.getExp(h.name);
            env.inContext = new Context(input.psi.get(expSym).c.pc, beginContext.lambda);
            PathOutcome ho = h.genConsVisit(env, tail_position);
            psi.join(ho);
            // cTry = new Context(Utils.makeJoin(cTry.pc, env.outContext.outPcName), Utils.makeJoin(cTry.lambda, env.outContext.lockName));
        }

        /*Utils.contextFlow(env, cTry, endContext, location);
        env.outContext = endContext;*/
        if (!tail_position) {
            env.cons.add(new Constraint(new Inequality(psi.getNormalPath().c.lambda, beginContext.lambda), env.hypothesis, location, env.curContractSym.name,
                    typecheck.Utils.ERROR_MESSAGE_LOCK_IN_NONLAST_OPERATION));
        }

        return psi;
    }
}

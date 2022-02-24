package ast;

import compile.SolCode;
import sherrlocUtils.Constraint;
import sherrlocUtils.Inequality;
import sherrlocUtils.Relation;
import typecheck.*;

import java.util.ArrayList;
import java.util.HashSet;

public class While extends NonFirstLayerStatement {
    Expression test;
    ArrayList<Statement> body;
    ArrayList<Statement> orelse; //TODO: ignoring for now
    public While(Expression test, ArrayList<Statement> body, ArrayList<Statement> orelse) {
        this.test = test;
        this.body = body;
        this.orelse = orelse;
    }
    public While(Expression test, ArrayList<Statement> body) {
        this.test = test;
        this.body = body;
        this.orelse = null;
    }

    public ScopeContext NTCgenCons(NTCEnv env, ScopeContext parent) {
        // consider to be a new scope
        // must contain at least one Statement
        ScopeContext now = new ScopeContext(this, parent);
        env.curSymTab = new SymTab(env.curSymTab);
        ScopeContext rtn = test.NTCgenCons(env, now);
        now.mergeExceptions(rtn);
        env.addCons(rtn.genCons(Utils.BuiltinType2ID(BuiltInT.BOOL), Relation.EQ, env, location));

        for (Statement s : body) {
            ScopeContext tmp = s.NTCgenCons(env, now);
            now.mergeExceptions(tmp);
        }
        for (Statement s : orelse) {
            ScopeContext tmp = s.NTCgenCons(env, now);
            now.mergeExceptions(tmp);
        }
        env.curSymTab = env.curSymTab.getParent();
        env.addCons(now.genCons(rtn, Relation.EQ, env, location));
        return now;
    }

    @Override
    public Context genConsVisit(VisitEnv env, boolean tail_position) {
        // String originalCtxt = env.ctxt;
        Context context = env.context;
        Context curContext = new Context(context.valueLabelName, Utils.getLabelNameLock(location), context.inLockName);

        // String prevLock = env.prevContext.lockName;

        Context testContext = test.genConsVisit(env, tail_position && body.size() == 0);
        String IfNameTestValue = testContext.valueLabelName;
        String IfNamePcBefore = Utils.getLabelNamePc(scopeContext.getParent().getSHErrLocName());
        // env.ctxt += ".While" + location.toString();
        String IfNamePcAfter = Utils.getLabelNamePc(scopeContext.getSHErrLocName());
        env.cons.add(new Constraint(new Inequality(IfNamePcBefore, IfNamePcAfter), env.hypothesis, location, env.curContractSym.name,
                "Control flow before this while statement contributes to the one in its scope"));

        env.cons.add(new Constraint(new Inequality(IfNameTestValue, IfNamePcAfter), env.hypothesis, location, env.curContractSym.name,
                "Integrity of the test condition contributes to control flow of this while statement"));

        env.incScopeLayer();
        // Context lastContext = new Context(testContext), prev2 = null;
        CodeLocation loc = null;
        for (Statement stmt : body) {
            env.context = curContext;
            Context tmp = stmt.genConsVisit(env, false);
            /*if (lastContext != null) {
                env.cons.add(new Constraint(new Inequality(tmp.lockName, Relation.LEQ, lastContext.lockName), env.hypothesis, loc, env.curContractSym.name,
                        Utils.ERROR_MESSAGE_LOCK_IN_NONLAST_OPERATION));
            }*/
            // env.prevContext = tmp;
            // prev2 = lastContext;
            loc = stmt.location;
            // lastContext = new Context(tmp);
        }
        env.decScopeLayer();
        env.cons.add(new Constraint(new Inequality(curContext.lockName, curContext.inLockName), env.hypothesis, location, env.curContractSym.name,
                Utils.ERROR_MESSAGE_LOCK_IN_NONLAST_OPERATION));

        // env.ctxt = originalCtxt;
        return curContext;
    }
    public void findPrincipal(HashSet<String> principalSet) {
        for (Statement stmt : body) {
            stmt.findPrincipal(principalSet);
        }
        for (Statement stmt : orelse) {
            stmt.findPrincipal(principalSet);
        }
    }

    @Override
    public void SolCodeGen(SolCode code) {
        code.enterWhile(test.toSolCode());
        for (Statement stmt : body) {
            if (stmt instanceof Expression) {
                ((Expression) stmt).SolCodeGenStmt(code);
            } else {
                stmt.SolCodeGen(code);
            }
        }
        code.leaveWhile();
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
        rtn.add(test);
        rtn.addAll(body);
        rtn.addAll(orelse);
        return rtn;
    }
}

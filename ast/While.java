package ast;

import compile.SolCode;
import typecheck.sherrlocUtils.Relation;
import typecheck.*;

import java.util.ArrayList;
import java.util.HashSet;

public class While extends Statement {

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

    public ScopeContext ntcGenCons(NTCEnv env, ScopeContext parent) {
        // consider to be a new scope
        // must contain at least one Statement
        ScopeContext now = new ScopeContext(this, parent);
        env.curSymTab = new SymTab(env.curSymTab);
        ScopeContext rtn = test.ntcGenCons(env, now);
        env.addCons(rtn.genCons(Utils.BuiltinType2ID(BuiltInT.BOOL), Relation.EQ, env, location));

        for (Statement s : body) {
            ScopeContext tmp = s.ntcGenCons(env, now);
        }
        for (Statement s : orelse) {
            ScopeContext tmp = s.ntcGenCons(env, now);
        }
        env.curSymTab = env.curSymTab.getParent();
        env.addCons(now.genCons(rtn, Relation.EQ, env, location));
        return now;
    }

    @Override
    public PathOutcome genConsVisit(VisitEnv env, boolean tail_position) {
        // TODO
        /*
        Context beginContext = env.inContext;
        Context endContext = new Context(Utils.getLabelNamePc(location),
                Utils.getLabelNameLock(location));

        Context testContext = test.genConsVisit(env, tail_position && body.size() == 0);
        String IfNameTestValue = testContext.valueLabelName;
        String IfNamePcBefore = Utils.getLabelNamePc(scopeContext.getParent().getSHErrLocName());
        String IfNamePcAfter = Utils.getLabelNamePc(scopeContext.getSHErrLocName());
        env.cons.add(new Constraint(new Inequality(IfNamePcBefore, IfNamePcAfter), env.hypothesis,
                location, env.curContractSym.name,
                "Control flow before this while statement contributes to the one in its scope"));

        env.cons.add(new Constraint(new Inequality(IfNameTestValue, IfNamePcAfter), env.hypothesis,
                location, env.curContractSym.name,
                "Integrity of the test condition contributes to control flow of this while statement"));

        env.incScopeLayer();
        CodeLocation loc = null;
        for (Statement stmt : body) {
            env.context = curContext;
            Context tmp = stmt.genConsVisit(env, false);
            loc = stmt.location;
        }
        env.decScopeLayer();
        env.cons.add(new Constraint(new Inequality(curContext.lambda, curContext.inLockName),
                env.hypothesis, location, env.curContractSym.name,
                Utils.ERROR_MESSAGE_LOCK_IN_NONLAST_OPERATION));

        return curContext;

         */
        return null;

    }
/*
    public void findPrincipal(HashSet<String> principalSet) {
        for (Statement stmt : body) {
            stmt.findPrincipal(principalSet);
        }
        for (Statement stmt : orelse) {
            stmt.findPrincipal(principalSet);
        }
    }*/

    @Override
    public void solidityCodeGen(SolCode code) {
        code.enterWhile(test.toSolCode());
        for (Statement stmt : body) {
            /*if (stmt instanceof Expression) {
                ((Expression) stmt).SolCodeGenStmt(code);
            } else {*/
            stmt.solidityCodeGen(code);
            // }
        }
        code.leaveWhile();
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
        rtn.add(test);
        rtn.addAll(body);
        rtn.addAll(orelse);
        return rtn;
    }
}

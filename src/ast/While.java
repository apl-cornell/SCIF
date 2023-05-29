package ast;

import compile.SolCode;
import java.nio.file.Path;
import java.util.List;
import typecheck.sherrlocUtils.Constraint;
import typecheck.sherrlocUtils.Inequality;
import typecheck.sherrlocUtils.Relation;
import typecheck.*;

import java.util.ArrayList;
import java.util.HashSet;

public class While extends Statement {

    Expression test;
    List<Statement> body;

    public While(Expression test, List<Statement> body) {
        this.test = test;
        this.body = body;
    }

    public ScopeContext ntcGenCons(NTCEnv env, ScopeContext parent) {
        // consider to be a new scope
        // must contain at least one Statement
        ScopeContext now = new ScopeContext(this, parent);
        env.enterNewScope();
        ScopeContext rtn = test.ntcGenCons(env, now);
        env.addCons(rtn.genCons(Utils.BuiltinType2ID(BuiltInT.BOOL), Relation.EQ, env, location));

        for (Statement s : body) {
            ScopeContext tmp = s.ntcGenCons(env, now);
        }
        env.exitNewScope();
        env.addCons(now.genCons(rtn, Relation.EQ, env, location));
        return now;
    }

    @Override
    public PathOutcome genConsVisit(VisitEnv env, boolean tail_position) {
        Context beginContext = env.inContext;
        Context endContext = new Context(Utils.getLabelNamePc(toSHErrLocFmt()),
                Utils.getLabelNameLock(toSHErrLocFmt()));

        ExpOutcome to = test.genConsVisit(env, false);

        String ifNameTest = to.valueLabelName;
        String ifNamePcBefore = Utils.getLabelNamePc(scopeContext.getParent().getSHErrLocName());
        String ifNamePcAfter = Utils.getLabelNamePc(scopeContext.getSHErrLocName());

        env.cons.add(new Constraint(new Inequality(ifNamePcBefore, ifNamePcAfter), env.hypothesis(),
                location, env.curContractSym().getName(),
                "Control flow before this while statement contributes to the one in its scope"));

        env.cons.add(new Constraint(new Inequality(ifNameTest, ifNamePcAfter), env.hypothesis(),
                location, env.curContractSym().getName(),
                "Integrity of the test condition contributes to control flow of this while statement"));

        env.incScopeLayer();

        env.inContext = new Context(ifNamePcAfter, beginContext.lambda);
        CodeLocation loc = null;
        PathOutcome ifo = to.psi;
        for (Statement stmt : body) {
            ifo = stmt.genConsVisit(env, false);

            PsiUnit normalUnit = ifo.getNormalPath();
            if (normalUnit == null) break;

            env.inContext = normalUnit.c;
            loc = stmt.location;
        }

        env.decScopeLayer();

        return ifo;

    }

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
        return rtn;
    }
}

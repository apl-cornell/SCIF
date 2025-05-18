package ast;

import compile.CompileEnv;
import java.util.ArrayList;
import java.util.List;
import typecheck.BuiltInT;
import typecheck.CodeLocation;
import typecheck.Context;
import typecheck.ExpOutcome;
import typecheck.NTCEnv;
import typecheck.PathOutcome;
import typecheck.PsiUnit;
import typecheck.ScopeContext;
import typecheck.Utils;
import typecheck.VisitEnv;
import typecheck.exceptions.SemanticException;
import typecheck.sherrlocUtils.Constraint;
import typecheck.sherrlocUtils.Inequality;
import typecheck.sherrlocUtils.Relation;

public class For extends Statement {
    AnnAssign newVars;
    Expression test;
    List<Statement> iter;
    List<Statement> body;

    public For(AnnAssign newVars, Expression test, List<Statement> iter, List<Statement> body) {
        this.newVars = newVars;
        this.test = test;
        this.iter = iter;
        this.body = body;
    }

    public ScopeContext genTypeConstraints(NTCEnv env, ScopeContext parent) throws SemanticException {
        ScopeContext now = new ScopeContext(this, parent);

        // create a new scope
        env.enterNewScope();

        newVars.genTypeConstraints(env, now);
        ScopeContext rtn = test.genTypeConstraints(env, now);
        // the test condition must be boolean
        env.addCons(rtn.genTypeConstraints(Utils.BuiltinType2ID(BuiltInT.BOOL), Relation.EQ, env, location));

        for (Statement s: body) {
            s.genTypeConstraints(env, now);
        }
        for (Statement s: iter) {
            s.genTypeConstraints(env, now);
        }

        env.exitNewScope();

        env.addCons(now.genTypeConstraints(rtn, Relation.EQ, env, location));
        return now;
    }

    public PathOutcome IFCVisit(VisitEnv env, boolean tail_position) throws SemanticException {
        Context beginContext = env.inContext;
        Context endContext = new Context(Utils.getLabelNamePc(toSHErrLocFmt()),
                Utils.getLabelNameLock(toSHErrLocFmt()));

        env.incScopeLayer();
        PathOutcome newVarOut = newVars.IFCVisit(env, false);
        beginContext = newVarOut.getNormalPath().c;
        ExpOutcome to = test.genIFConstraints(env, false);

        String ifNameTest = to.valueLabelName;
        String ifNamePcBefore = Utils.getLabelNamePc(scopeContext.getParent().getSHErrLocName());
        String ifNamePcAfter = Utils.getLabelNamePc(scopeContext.getSHErrLocName());

        env.cons.add(new Constraint(new Inequality(ifNamePcBefore, ifNamePcAfter), env.hypothesis(),
                location, env.curContractSym().getName(),
                "Control flow before this for statement contributes to the one in its scope"));

        env.cons.add(new Constraint(new Inequality(ifNameTest, ifNamePcAfter), env.hypothesis(),
                location, env.curContractSym().getName(),
                "Integrity of the test condition contributes to control flow of this for statement"));


        env.inContext = new Context(ifNamePcAfter, beginContext.lambda);
        CodeLocation loc = null;
        PathOutcome ifo = to.psi;
        for (Statement stmt : body) {
            ifo = stmt.IFCVisit(env, false);

            PsiUnit normalUnit = ifo.getNormalPath();
            if (normalUnit == null) break;

            env.inContext = normalUnit.c;
            loc = stmt.location;
        }
        for (Statement s: iter) {
            ifo = s.IFCVisit(env, false);

            PsiUnit normalUnit = ifo.getNormalPath();
            if (normalUnit == null) break;

            env.inContext = normalUnit.c;
            loc = s.location;
        }

        env.decScopeLayer();

        return ifo;

    }

    @Override
    public List<compile.ast.Statement> solidityCodeGen(CompileEnv code) {
        assert false;
        return null;
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
        rtn.add(newVars);
        rtn.add(test);
        rtn.addAll(iter);
        rtn.addAll(body);
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
}

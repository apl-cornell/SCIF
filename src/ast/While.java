package ast;

import compile.CompileEnv;
import compile.ast.Type;
import compile.ast.VarDec;
import compile.ast.WhileStatement;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import typecheck.exceptions.SemanticException;
import typecheck.sherrlocUtils.Constraint;
import typecheck.sherrlocUtils.Inequality;
import typecheck.sherrlocUtils.Relation;
import typecheck.*;

import java.util.ArrayList;

public class While extends Statement {

    Expression test;
    List<Statement> body;

    public While(Expression test, List<Statement> body) {
        this.test = test;
        this.body = body;
    }

    public ScopeContext generateConstraints(NTCEnv env, ScopeContext parent) throws SemanticException {
        // consider to be a new scope
        // must contain at least one Statement
        ScopeContext now = new ScopeContext(this, parent);
        env.enterNewScope();
        ScopeContext rtn = test.generateConstraints(env, now);
        env.addCons(rtn.genCons(Utils.BuiltinType2ID(BuiltInT.BOOL), Relation.EQ, env, location));

        for (Statement s : body) {
            ScopeContext tmp = s.generateConstraints(env, now);
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
        Utils.genConsStatments(body, env, ifo, false);
//        for (Statement stmt : body) {
//            ifo = stmt.genConsVisit(env, false);
//
//            PsiUnit normalUnit = ifo.getNormalPath();
//            if (normalUnit == null) break;
//
//            env.inContext = normalUnit.c;
//            loc = stmt.location;
//        }

        env.decScopeLayer();

        return ifo;

    }

    @Override
    public List<compile.ast.Statement> solidityCodeGen(CompileEnv code) {
        List<compile.ast.Statement> result = new ArrayList<>();
        List<compile.ast.Statement> condPrep = new ArrayList<>();
        List<compile.ast.Statement> whileBody = new ArrayList<>();
        compile.ast.Expression cond = test.solidityCodeGen(condPrep, code);
        result.addAll(condPrep);
        code.enterNewVarScope();
        for (Statement stmt : body) {
            whileBody.addAll(stmt.solidityCodeGen(code));
        }

        // add prep the end of whileBody excluding declarations
        for (compile.ast.Statement s: condPrep) {
            if (!(s instanceof VarDec)) {
                whileBody.add(s);
            }
        }
        code.exitVarScope();
        result.add(new WhileStatement(cond, whileBody));
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
        rtn.add(test);
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

    @Override
    protected java.util.Map<String,? extends Type> readMap(CompileEnv code) {
        java.util.Map<String, Type> result = test.readMap(code);
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

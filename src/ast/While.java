package ast;

import compile.CompileEnv;
import compile.ast.Type;
import compile.ast.VarDec;
import compile.ast.WhileStatement;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import typecheck.exceptions.SemanticException;
import typecheck.sherrlocUtils.Relation;
import typecheck.*;

import java.util.ArrayList;

public class While extends Statement {

    Expression test;
    Block body;

    public While(Expression test, List<Statement> body) {
        this.test = test;
        this.body = new Block(body);
    }

    public ScopeContext genTypeConstraints(NTCEnv env, ScopeContext parent) throws SemanticException {
        // consider to be a new scope
        // must contain at least one Statement
        ScopeContext now = new ScopeContext(this, parent);
        env.enterNewScope();
        ScopeContext rtn = test.genTypeConstraints(env, now);
        env.addCons(rtn.genTypeConstraints(Utils.BuiltinType2ID(BuiltInT.BOOL), Relation.EQ, env, location));

        body.genTypeConstraints(env, now);
        env.exitNewScope();
        env.addCons(now.genTypeConstraints(rtn, Relation.EQ, env, location));
        return now;
    }

    @Override
    public PathOutcome IFCVisit(VisitEnv env, boolean tail_position) throws SemanticException {
        Context beginContext = env.inContext;
        Context endContext = new Context(Utils.getLabelNamePc(toSHErrLocFmt()),
                Utils.getLabelNameLock(toSHErrLocFmt()));

        // The loop head is reached from before the loop and from the end of the body.
        java.util.Set<String> assigned = Assign.assignedNames(body.statements());
        env.havoc(assigned);
        ExpOutcome to = test.genIFConstraints(env, false);

        String ifNameTest = to.valueLabelName;
        String ifNamePcBefore = Utils.getLabelNamePc(scopeContext.getParent().getSHErrLocName());
        String ifNamePcAfter = Utils.getLabelNamePc(scopeContext.getSHErrLocName());

        env.checkFlow(ifNamePcBefore, ifNamePcAfter, location,
                "Control flow before this while statement contributes to the one in its scope");

        env.checkFlow(ifNameTest, ifNamePcAfter, location,
                "Integrity of the test condition contributes to control flow of this while statement");

        env.incScopeLayer();

        env.inContext = new Context(ifNamePcAfter, beginContext.lambda);
        CodeLocation loc = null;
        PathOutcome bodyOutcome = body.IFCVisit(env, false);
        // The loop exits only from its head, which is also reached from the end of the body.
        PathOutcome ifo = to.psi.withoutNormalPath();
        ifo.join(bodyOutcome.withoutNormalPath());
        if (bodyOutcome.getNormalPath() instanceof PsiUnit bodyEnd) {
            env.checkFlow(bodyEnd.c.pc, ifNamePcAfter, location,
                    "Control flow at the end of the loop body contributes to the control flow of this while statement");
        }
        ifo.setNormalPath(new Context(ifNamePcAfter, to.psi.getNormalPath().c.lambda));
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
        // The loop exits from its head or from a break in the body.
        env.havoc(assigned);

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
        whileBody.addAll(body.solidityCodeGen(code));

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
        rtn.add(body);
        return rtn;
    }
    @Override
    public boolean exceptionHandlingFree() {
        return body.exceptionHandlingFree();
    }

    @Override
    protected java.util.Map<String,? extends Type> readMap(CompileEnv code) {
        java.util.Map<String, Type> result = test.readMap(code);
        result.putAll(body.readMap(code));
        return result;
    }

    @Override
    protected java.util.Map<String,? extends Type> writeMap(CompileEnv code) {
        return body.writeMap(code);
    }
}

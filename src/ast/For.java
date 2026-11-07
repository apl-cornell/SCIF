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
import typecheck.sherrlocUtils.Relation;

public class For extends Statement {
    AnnAssign newVars;
    Expression test;
    Block iter;
    Block body;

    public For(AnnAssign newVars, Expression test, List<Statement> iter, List<Statement> body) {
        this.newVars = newVars;
        this.test = test;
        this.iter = new Block(iter);
        this.body = new Block(body);
    }

    public ScopeContext genTypeConstraints(NTCEnv env, ScopeContext parent) throws SemanticException {
        ScopeContext now = new ScopeContext(this, parent);

        // create a new scope
        env.enterNewScope();

        newVars.genTypeConstraints(env, now);
        ScopeContext rtn = test.genTypeConstraints(env, now);
        // the test condition must be boolean
        env.addCons(rtn.genTypeConstraints(Utils.BuiltinType2ID(BuiltInT.BOOL), Relation.EQ, env, location, "The loop condition must be a bool"));

        body.genTypeConstraints(env, now);
        iter.genTypeConstraints(env, now);

        env.exitNewScope();

        env.addCons(now.genTypeConstraints(rtn, Relation.EQ, env, location, "A for statement has the type of its condition"));
        return now;
    }

    public PathOutcome IFCVisit(VisitEnv env, boolean tail_position) throws SemanticException {
        Context beginContext = env.inContext;
        Context endContext = new Context(Utils.getLabelNamePc(toSHErrLocFmt()),
                Utils.getLabelNameLock(toSHErrLocFmt()));

        env.incScopeLayer();
        PathOutcome newVarOut = newVars.IFCVisit(env, false);
        beginContext = newVarOut.getNormalPath().c;
        // The loop head is reached from before the loop and from the end of an iteration.
        java.util.Set<String> assigned = Assign.assignedNames(List.of(body, iter));
        env.havoc(assigned);
        ExpOutcome to = test.genIFConstraints(env, false);

        String ifNameTest = to.valueLabelName;
        String ifNamePcBefore = Utils.getLabelNamePc(scopeContext.getParent().getSHErrLocName());
        String ifNamePcAfter = Utils.getLabelNamePc(scopeContext.getSHErrLocName());

        env.checkFlow(ifNamePcBefore, ifNamePcAfter, location,
                "Control flow in the loop is trusted no more than control flow before it");

        env.checkFlow(ifNameTest, ifNamePcAfter, location,
                "Control flow in the loop is trusted no more than the loop condition");


        env.inContext = new Context(ifNamePcAfter, beginContext.lambda);
        CodeLocation loc = null;
        List<Statement> iteration = new ArrayList<>(body.statements());
        iteration.addAll(iter.statements());
        PathOutcome iterOutcome = new Block(iteration).IFCVisit(env, false);
        // The loop exits only from its head, which is also reached from the end of an
        // iteration.
        PathOutcome ifo = to.psi.withoutNormalPath();
        ifo.join(iterOutcome.withoutNormalPath());
        if (iterOutcome.getNormalPath() instanceof PsiUnit iterEnd) {
            env.checkFlow(iterEnd.c.pc, ifNamePcAfter, location,
                    "Control flow in the loop is trusted no more than control flow at the end of the loop body");
        }
        ifo.setNormalPath(new Context(ifNamePcAfter, to.psi.getNormalPath().c.lambda));

        // The loop exits from its head or from a break in the body.
        env.havoc(assigned);
        env.decScopeLayer();

        return ifo;

    }

    @Override
    public List<compile.ast.Statement> solidityCodeGen(CompileEnv code) {
        throw new AssertionError();
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
        rtn.add(iter);
        rtn.add(body);
        return rtn;
    }

    @Override
    public boolean exceptionHandlingFree() {
        return body.exceptionHandlingFree();
    }
}

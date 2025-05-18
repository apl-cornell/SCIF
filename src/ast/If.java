package ast;

import compile.CompileEnv;
import compile.ast.IfStatement;
import compile.ast.Type;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import typecheck.exceptions.SemanticException;
import typecheck.sherrlocUtils.Constraint;
import typecheck.sherrlocUtils.Inequality;
import typecheck.sherrlocUtils.Relation;
import typecheck.*;

import java.util.ArrayList;

public class If extends Statement {

    Expression test;
    List<Statement> body;
    List<Statement> orelse;

    public If(Expression test, List<Statement> body, List<Statement> orelse) {
        this.test = test;
        this.body = body;
        this.orelse = orelse;
    }

    public If(Expression test, List<Statement> body) {
        this.test = test;
        this.body = body;
        this.orelse = new ArrayList<>();
    }

    public ScopeContext genTypeConstraints(NTCEnv env, ScopeContext parent) throws SemanticException {
        // consider to be a new scope
        // must contain at least one Statement
        ScopeContext now = scopeContext;
        ScopeContext rtn = test.genTypeConstraints(env, now);
        Constraint testCon = rtn.genTypeConstraints(Utils.BuiltinType2ID(BuiltInT.BOOL), Relation.EQ, env, test.location);
        env.addCons(testCon);

        env.enterNewScope();
        for (Statement s : body) {
            assert !now.getSHErrLocName().startsWith("null"): now.getSHErrLocName();
            rtn = s.genTypeConstraints(env, now);
        }
        env.exitNewScope();
        env.enterNewScope();
        for (Statement s : orelse) {
            rtn = s.genTypeConstraints(env, now);
        }
        env.exitNewScope();
        env.addCons(now.genTypeConstraints(rtn, Relation.EQ, env, location));
        return now;
    }

    @Override
    public PathOutcome IFCVisit(VisitEnv env, boolean tail_position) throws SemanticException {
        Context beginContext = env.inContext;
        Context endContext = new Context(typecheck.Utils.getLabelNamePc(toSHErrLocFmt()),
                typecheck.Utils.getLabelNameLock(toSHErrLocFmt()));
        // String originalCtxt = env.ctxt;

        // curContext.lambda = env.prevContext.lambda;
        String rtnValueLabel;

        ExpOutcome to = test.genIFConstraints(env,
                body.size() == 0 && orelse.size() == 0 && tail_position);
        //Context condContext = to.psi
        rtnValueLabel = to.valueLabelName;
        String IfNameTest = to.valueLabelName;
        String IfTestNormalPc = to.psi.getNormalPath().c.pc;
        String IfTestNormalLambda = to.psi.getNormalPath().c.lambda;
        String IfNamePcBefore = Utils.getLabelNamePc(scopeContext.getParent().getSHErrLocName());
        // env.ctxt += ".If" + location.toString();
        String IfNamePcAfter = Utils.getLabelNamePc(scopeContext.getSHErrLocName());

        boolean createdHypo = false;
        //TestableVarInfo testedVar = null;
        //String beforeTestedLabel = "";
        boolean tested = false;
        if (test instanceof FlowsToExp fte) {
            Name left = (Name) fte.lhs, right = (Name) fte.rhs;
            if (env.containsVar(left.id) && env.containsVar(right.id)) {

                logger.debug("if both exists");
                VarSym l = env.getVar(left.id), r = env.getVar(right.id);
                logger.debug(l.toString());
                logger.debug(r.toString());
                createdHypo = true;
                Inequality hypo = new Inequality(l.toSHErrLocFmt(), Relation.LEQ,
                        r.toSHErrLocFmt());

                env.hypothesis().add(hypo);
                logger.debug("testing label");
            }
        }
        env.cons.add(new Constraint(new Inequality(IfNamePcBefore, IfNamePcAfter), env.hypothesis(),
                location, env.curContractSym().getName(),
                "Control flow integrity before this if condition doesn't flow to the one after this condition"));
        env.cons.add(
                new Constraint(new Inequality(IfNameTest, IfNamePcAfter), env.hypothesis(), location,
                        env.curContractSym().getName(),
                        "Integrity of this condition doesn't flow to the control flow in this if branch"));

        if (body.size() > 0 || orelse.size() > 0) {
            Utils.genSequenceConstraints(env, beginContext.lambda, IfTestNormalPc, IfTestNormalLambda, IfNamePcAfter, test.location);
//            env.cons.add(new Constraint(new Inequality(IfTestNormalPc, IfNamePcAfter), env.hypothesis(), test.location, env.curContractSym().getName(),
//                    Utils.ERROR_MESSAGE_LOCK_IN_NONLAST_OPERATION));
        }
        env.incScopeLayer();

        // Context leftContext = new Context(curContext), rightContext = new Context(curContext);
        CodeLocation loc = null;
        PathOutcome ifo = to.psi;
        env.inContext = new Context(IfNamePcAfter, beginContext.lambda);
        int index = 0;
        for (Statement stmt : body) {
            ++index;
            String prevLambda = env.inContext.lambda;
            ifo = stmt.IFCVisit(env, index == body.size() && tail_position);
            // env.prevContext = tmp;
            // prev2 = leftContext;
            //env.context = context;
            PsiUnit normalUnit = ifo.getNormalPath();
            if (normalUnit == null) {
                break;
            }
            env.inContext = Utils.genNewContextAndConstraints(env, index == body.size() && tail_position, normalUnit.c, prevLambda, stmt.nextPcSHL(), stmt.location);
            loc = stmt.location;
            // leftContext = new Context(tmp);
        }
        env.decScopeLayer();

        if (createdHypo) {
            env.hypothesis().pop();
        }

        logger.debug("finished if branch");
        //System.err.println("finished if branch");
        // env.prevContext.lambda = curContext.lambda;
        env.incScopeLayer();
        index = 0;
        PathOutcome elseo = to.psi;
        env.inContext = new Context(IfNamePcAfter, beginContext.lambda);
        for (Statement stmt : orelse) {
            ++index;
            String prevLambda = env.inContext.lambda;
            elseo = stmt.IFCVisit(env, index == orelse.size() && tail_position);
            PsiUnit normalUnit = elseo.getNormalPath();
            if (normalUnit == null) {
                break;
            }
            env.inContext = Utils.genNewContextAndConstraints(env, index == body.size() && tail_position, normalUnit.c, prevLambda, stmt.nextPcSHL(), stmt.location);
            // env.prevContext.lambda = rightContext.lambda;
        }
        env.decScopeLayer();

        ifo.join(elseo);
        // env.cons.add(new Constraint(new Inequality(IfNameLock, Relation.REQ, Utils.joinLabels(leftContext.lambda, rightContext.lambda)), env.hypothesis, location));

        /*env.cons.add(new Constraint(new Inequality(IfNameLock, Relation.GEQ, leftContext.lambda), env.hypothesis, location, env.curContractSym.name,
                "Lock of then branch contributes to lock of this if statement"));
        env.cons.add(new Constraint(new Inequality(IfNameLock, Relation.GEQ, rightContext.lambda), env.hypothesis, location, env.curContractSym.name,
                "Lock of else branch contributes to lock of this if statement"));*/

        logger.debug("finished orelse branch");

        return ifo;
    }

    @Override
    public List<compile.ast.Statement> solidityCodeGen(CompileEnv code) {
        List<compile.ast.Statement> result = new ArrayList<>();
        List<compile.ast.Statement> ifBranch = new ArrayList<>();
        compile.ast.Expression cond = test.solidityCodeGen(result, code);
        code.enterNewVarScope();
        for (Statement stmt : body) {
            ifBranch.addAll(stmt.solidityCodeGen(code));
        }
        code.exitVarScope();
        if (!orelse.isEmpty()) {
            List<compile.ast.Statement> elseBranch = new ArrayList<>();
            code.enterNewVarScope();
            for (Statement stmt : orelse) {
                elseBranch.addAll(stmt.solidityCodeGen(code));
            }
            code.exitVarScope();
            result.add(new IfStatement(cond, ifBranch, elseBranch));
        } else {
            result.add(new IfStatement(cond, ifBranch));
        }
        return result;
    }

    @Override
    public void passScopeContext(ScopeContext parent) {
        scopeContext = new ScopeContext(this, parent);
        test.passScopeContext(parent);
        for (Node node : children()) {
            node.passScopeContext(scopeContext);
        }
    }

    @Override
    public List<Node> children() {
        ArrayList<Node> rtn = new ArrayList<>();
        rtn.addAll(body);
        rtn.addAll(orelse);
        return rtn;
    }
    @Override
    public boolean exceptionHandlingFree() {
        for (Statement s: body) {
            if (!s.exceptionHandlingFree()) {
                return false;
            }
        }
        for (Statement s: orelse) {
            if (!s.exceptionHandlingFree()) {
                return false;
            }
        }
        return true;
    }

    @Override
    protected java.util.Map<String,? extends Type> readMap(CompileEnv code) {
        java.util.Map<String, Type> result = new HashMap<>();
        for (Statement s: body) {
            result.putAll(s.readMap(code));
        }
        for (Statement s: orelse) {
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
        for (Statement s: orelse) {
            result.putAll(s.writeMap(code));
        }
        return result;
    }
}

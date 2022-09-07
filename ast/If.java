package ast;

import compile.SolCode;
import java.util.List;
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

    public ScopeContext ntcGenCons(NTCEnv env, ScopeContext parent) {
        // consider to be a new scope
        // must contain at least one Statement
        ScopeContext now = new ScopeContext(this, parent);
        env.curSymTab = new SymTab(env.curSymTab);
        ScopeContext rtn = null;

        rtn = test.ntcGenCons(env, now);
        env.addCons(rtn.genCons(Utils.BuiltinType2ID(BuiltInT.BOOL), Relation.EQ, env, location));

        for (Statement s : body) {
            rtn = s.ntcGenCons(env, now);
        }
        for (Statement s : orelse) {
            rtn = s.ntcGenCons(env, now);
        }
        env.curSymTab = env.curSymTab.getParent();
        env.addCons(now.genCons(rtn, Relation.EQ, env, location));
        return now;
    }

    @Override
    public PathOutcome genConsVisit(VisitEnv env, boolean tail_position) {
        Context beginContext = env.inContext;
        Context endContext = new Context(typecheck.Utils.getLabelNamePc(location),
                typecheck.Utils.getLabelNameLock(location));
        // String originalCtxt = env.ctxt;

        // curContext.lambda = env.prevContext.lambda;
        String rtnValueLabel;

        ExpOutcome to = test.genConsVisit(env,
                body.size() == 0 && orelse.size() == 0 && tail_position);
        //Context condContext = to.psi
        rtnValueLabel = to.valueLabelName;
        String IfNameTest = to.valueLabelName;
        String IfNamePcBefore = Utils.getLabelNamePc(scopeContext.getParent().getSHErrLocName());
        // env.ctxt += ".If" + location.toString();
        String IfNamePcAfter = Utils.getLabelNamePc(scopeContext.getSHErrLocName());

        boolean createdHypo = false;
        //TestableVarInfo testedVar = null;
        //String beforeTestedLabel = "";
        boolean tested = false;
        if (test instanceof Compare) {
            Compare bo = (Compare) test;
            if ((bo.op == CompareOperator.Eq || bo.op == CompareOperator.GtE
                    || bo.op == CompareOperator.LtE) &&
                    bo.left instanceof Name && bo.right instanceof Name) {
                Name left = (Name) bo.left, right = (Name) bo.right;
                if (env.containsVar(left.id) && env.containsVar(right.id)) {

                    logger.debug("if both exists");
                    //System.err.println("if both exists");
                    VarSym l = env.getVar(left.id), r = env.getVar(right.id);
                    logger.debug(l.toString());
                    logger.debug(r.toString());
                    //System.err.println(l.toString());
                    //System.err.println(r.toString());
                    if (l.typeSym.name.equals(Utils.ADDRESSTYPE) && r.typeSym.name.equals(
                            Utils.ADDRESSTYPE)) {
                        /*testedVar = ((TestableVarInfo) l);
                        beforeTestedLabel = testedVar.testedLabel;
                        tested = testedVar.tested;
                        testedVar.setTested(r.toSherrlocFmt());*/

                        createdHypo = true;
                        Inequality hypo = new Inequality(l.toSherrlocFmt(), bo.op,
                                r.toSherrlocFmt());

                        env.hypothesis.add(hypo);
                        //System.err.println("testing label");
                        logger.debug("testing label");
                    }
                } else {
                    //TODO: cannot find both the variables
                }
            }
        }
        env.cons.add(new Constraint(new Inequality(IfNamePcBefore, IfNamePcAfter), env.hypothesis,
                location, env.curContractSym.name,
                "Control flow integrity before this if condition doesn't flow to the one after this condition"));
        env.cons.add(
                new Constraint(new Inequality(IfNameTest, IfNamePcAfter), env.hypothesis, location,
                        env.curContractSym.name,
                        "Integrity of this condition doesn't flow to the control flow in this if branch"));

        /*if (body.size() > 0 || orelse.size() > 0) {
            env.cons.add(new Constraint(new Inequality(prevLockLabel, Relation.GEQ, curContext.lambda), env.hypothesis, test.location, env.curContractSym.name,
                    Utils.ERROR_MESSAGE_LOCK_IN_NONLAST_OPERATION));
        }*/
        env.incScopeLayer();

        // Context leftContext = new Context(curContext), rightContext = new Context(curContext);
        CodeLocation loc = null;
        PathOutcome ifo = to.psi;
        env.inContext = new Context(IfNamePcAfter, beginContext.lambda);
        int index = 0;
        for (Statement stmt : body) {
            ++index;
            ifo = stmt.genConsVisit(env, index == body.size() && tail_position);
            // env.prevContext = tmp;
            // prev2 = leftContext;
            //env.context = context;
            env.inContext = ifo.getNormalPath().c;
            loc = stmt.location;
            // leftContext = new Context(tmp);
        }
        env.decScopeLayer();

        if (createdHypo) {
            env.hypothesis.remove();
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
            elseo = stmt.genConsVisit(env, index == orelse.size() && tail_position);
            env.inContext = elseo.getNormalPath().c;
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
        //System.err.println("finished orelse branch");

        // env.ctxt = originalCtxt;
        return ifo;
    }

    @Override
    public void solidityCodeGen(SolCode code) {
        String cond = test.toSolCode();
        code.enterIf(cond);
        for (Statement stmt : body) {
            /*if (stmt instanceof Expression) {
                ((Expression) stmt).SolCodeGenStmt(code);
            } else {*/
            stmt.solidityCodeGen(code);
            //}
        }
        code.leaveIf();
        if (!orelse.isEmpty()) {
            code.enterElse();
            for (Statement stmt : orelse) {
                /*if (stmt instanceof Expression) {
                    ((Expression) stmt).SolCodeGenStmt(code);
                } else {*/
                stmt.solidityCodeGen(code);
                //}
            }
            code.leaveElse();
        }
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

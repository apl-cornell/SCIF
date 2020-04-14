package ast;

import compile.SolCode;
import sherrlocUtils.Constraint;
import sherrlocUtils.Inequality;
import sherrlocUtils.Relation;
import typecheck.*;

import java.util.ArrayList;

public class If extends Statement {
    Expression test;
    ArrayList<Statement> body;
    ArrayList<Statement> orelse;
    public If(Expression test, ArrayList<Statement> body, ArrayList<Statement> orelse) {
        this.test = test;
        this.body = body;
        this.orelse = orelse;
    }
    public If(Expression test, ArrayList<Statement> body) {
        this.test = test;
        this.body = body;
        this.orelse = new ArrayList<>();
    }

    public ScopeContext NTCgenCons(NTCEnv env, ScopeContext parent) {
        // consider to be a new scope
        // must contain at least one Statement
        ScopeContext now = new ScopeContext(this, parent);
        env.curSymTab = new SymTab(env.curSymTab);
        ScopeContext rtn = null;

        rtn = test.NTCgenCons(env, now);
        env.addCons(rtn.genCons(Utils.BuiltinType2ID(BuiltInT.BOOL), Relation.EQ, env, location));

        for (Statement s : body) {
            rtn = s.NTCgenCons(env, now);
        }
        for (Statement s : orelse) {
            rtn = s.NTCgenCons(env, now);
        }
        env.curSymTab = env.curSymTab.getParent();
        env.addCons(now.genCons(rtn, Relation.EQ, env, location));
        return now;
    }

    @Override
    public Context genConsVisit(VisitEnv env) {
        // String originalCtxt = env.ctxt;

        String prevLockLabel = env.prevContext.lockName;
        String rtnValueLabel;

        Context curContext = test.genConsVisit(env);
        rtnValueLabel = curContext.valueLabelName;
        String IfNameTest = curContext.valueLabelName;
        String IfNamePcBefore = Utils.getLabelNamePc(env.ctxt.getParent().getSHErrLocName());
        // env.ctxt += ".If" + location.toString();
        String IfNamePcAfter = Utils.getLabelNamePc(env.ctxt.getSHErrLocName());
        String IfNameLock = Utils.getLabelNameLock(env.ctxt.getSHErrLocName());


        boolean createdHypo = false;
        //TestableVarInfo testedVar = null;
        //String beforeTestedLabel = "";
        boolean tested = false;
        if (test instanceof Compare) {
            Compare bo = (Compare) test;
            if ((bo.op == CompareOperator.Eq || bo.op == CompareOperator.GtE || bo.op == CompareOperator.LtE) &&
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
                    if (l.typeSym.name.equals(Utils.ADDRESSTYPE) && r.typeSym.name.equals(Utils.ADDRESSTYPE)) {
                        /*testedVar = ((TestableVarInfo) l);
                        beforeTestedLabel = testedVar.testedLabel;
                        tested = testedVar.tested;
                        testedVar.setTested(r.toSherrlocFmt());*/

                        createdHypo = true;
                        Inequality hypo = new Inequality(l.toSherrlocFmt(), bo.op, r.toSherrlocFmt());

                        env.hypothesis.add(hypo);
                        //System.err.println("testing label");
                        logger.debug("testing label");
                    }
                } else {
                    //TODO: cannot find both the variables
                }
            }
        }
        env.cons.add(new Constraint(new Inequality(IfNamePcBefore, IfNamePcAfter), env.hypothesis, location));
        env.cons.add(new Constraint(new Inequality(IfNameTest, IfNamePcAfter), env.hypothesis, location));

        if (body.size() > 0 || orelse.size() > 0) {
            env.cons.add(new Constraint(new Inequality(prevLockLabel, Relation.EQ, curContext.lockName), env.hypothesis, location));
        }
        env.incScopeLayer();

        Context leftContext = new Context(curContext), rightContext = new Context(curContext), prev2 = null;
        for (Statement stmt : body) {
            if (prev2 != null) {
                env.cons.add(new Constraint(new Inequality(leftContext.lockName, Relation.EQ, prev2.lockName), env.hypothesis, location));
            }
            Context tmp = stmt.genConsVisit(env);
            env.prevContext = tmp;
            prev2 = leftContext;
            leftContext = new Context(tmp);
        }
        env.decScopeLayer();

        if (createdHypo) {
            env.hypothesis.remove();
        }

        logger.debug("finished if branch");
        //System.err.println("finished if branch");
        env.prevContext.lockName = curContext.lockName;
        env.incScopeLayer();
        for (Statement stmt : orelse) {
            rightContext = stmt.genConsVisit(env);
            env.prevContext.lockName = rightContext.lockName;
        }
        env.decScopeLayer();


        env.cons.add(new Constraint(new Inequality(IfNameLock, Relation.EQ, Utils.joinLabels(leftContext.lockName, rightContext.lockName)), env.hypothesis, location));

        logger.debug("finished orelse branch");
        //System.err.println("finished orelse branch");

        // env.ctxt = originalCtxt;
        return new Context(null, IfNameLock);
    }

    @Override
    public void SolCodeGen(SolCode code) {
        String cond = test.toSolCode();
        code.enterIf(cond);
        for (Statement stmt : body) {
            stmt.SolCodeGen(code);
        }
        code.leaveIf();
        if (!orelse.isEmpty()) {
            code.enterElse();
            for (Statement stmt : orelse) {
                stmt.SolCodeGen(code);
            }
            code.leaveElse();
        }
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

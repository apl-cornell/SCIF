package ast;

import sherrlocUtils.Constraint;
import sherrlocUtils.Inequality;
import typecheck.*;

import java.util.ArrayList;
import java.util.HashMap;

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

    @Override
    public String genConsVisit(VisitEnv env) {
        String originalCtxt = env.ctxt;

        String IfNameTest = test.genConsVisit(env);
        String IfNamePcBefore = Utils.getLabelNamePc(env.ctxt);
        env.ctxt += ".If" + location.toString();
        String IfNamePcAfter = Utils.getLabelNamePc(env.ctxt);


        boolean createdHypo = false;
        //TestableVarInfo testedVar = null;
        //String beforeTestedLabel = "";
        boolean tested = false;
        if (test instanceof Compare) {
            Compare bo = (Compare) test;
            if ((bo.op == CompareOperator.Eq || bo.op == CompareOperator.GtE || bo.op == CompareOperator.LtE) &&
                bo.left instanceof Name && bo.right instanceof Name) {
                Name left = (Name) bo.left, right = (Name) bo.right;
                if (env.varNameMap.exists(left.id) && env.varNameMap.exists(right.id)) {

                    logger.debug("if both exists");
                    //System.err.println("if both exists");
                    VarInfo l = env.varNameMap.getInfo(left.id), r = env.varNameMap.getInfo(right.id);
                    logger.debug(l.toString());
                    logger.debug(r.toString());
                    //System.err.println(l.toString());
                    //System.err.println(r.toString());
                    if (l.typeInfo.type.typeName.equals(Utils.ADDRESSTYPE) && r.typeInfo.type.typeName.equals(Utils.ADDRESSTYPE)) {
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

        env.varNameMap.incLayer();
        for (Statement stmt : body) {
            stmt.genConsVisit(env);
        }
        env.varNameMap.decLayer();

        if (createdHypo) {
            env.hypothesis.remove();
        }

        logger.debug("finished if branch");
        //System.err.println("finished if branch");

        env.varNameMap.incLayer();
        for (Statement stmt : orelse) {
            stmt.genConsVisit(env);
        }
        env.varNameMap.decLayer();

        logger.debug("finished orelse branch");
        //System.err.println("finished orelse branch");

        env.ctxt = originalCtxt;
        return null;
    }
}

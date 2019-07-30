package ast;

import utils.*;

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
    public String genConsVisit(String ctxt, HashMap<String, FuncInfo> funcMap, ArrayList<IfConstraint> cons, LookupMaps varNameMap) {
        String IfNameTest = test.genConsVisit(ctxt, funcMap, cons, varNameMap);
        String IfNamePcBefore = Utils.getLabelNamePc(ctxt);
        ctxt += ".If" + location.toString();
        String IfNamePcAfter = Utils.getLabelNamePc(ctxt);
        cons.add(Utils.genCons(IfNamePcBefore, IfNamePcAfter, location));
        cons.add(Utils.genCons(IfNameTest, IfNamePcAfter, test.location));

        TestableVarInfo testedVar = null;
        String beforeTestedLabel = "";
        boolean tested = false;
        if (test instanceof Compare) {
            Compare bo = (Compare) test;
            if (bo.op == CompareOperator.Eq &&
                bo.left instanceof Name && bo.right instanceof Name) {
                Name left = (Name) bo.left, right = (Name) bo.right;
                if (varNameMap.exists(left.id) && varNameMap.exists(right.id)) {

                    System.err.println("if both exists");
                    VarInfo l = varNameMap.getInfo(left.id), r = varNameMap.getInfo(right.id);
                    System.err.println(l.toString());
                    System.err.println(r.toString());
                    if (l instanceof TestableVarInfo && r.type.isConst && r.type.typeName.equals(Utils.ADDRESSTYPE)) {
                        testedVar = ((TestableVarInfo) l);
                        beforeTestedLabel = testedVar.testedLabel;
                        tested = testedVar.tested;
                        testedVar.setTested(r.toSherrlocFmt());
                        System.err.println("testing label var");
                    }
                } else {
                    //TODO: cannot find both the variables
                }
            }
        }

        varNameMap.incLayer();
        for (Statement stmt : body) {
            stmt.genConsVisit(ctxt, funcMap, cons, varNameMap);
        }
        varNameMap.decLayer();

        String ifTestedLabel = "";
        if (testedVar != null) {
            ifTestedLabel = testedVar.testedLabel;
            testedVar.testedLabel = beforeTestedLabel;
            testedVar.tested = tested;
        }
        System.err.println("finished if branch");

        varNameMap.incLayer();
        for (Statement stmt : orelse) {
            stmt.genConsVisit(ctxt, funcMap, cons, varNameMap);
        }
        varNameMap.decLayer();

        System.err.println("finished orelse branch");
        if (testedVar != null) {
            if ((ifTestedLabel == null || ifTestedLabel.equals(Utils.DEAD)) || (testedVar.testedLabel == null || testedVar.testedLabel.equals(Utils.DEAD))) {
                testedVar.testedLabel = Utils.DEAD;
                testedVar.tested = false;
            }
        }
        return null;
    }
}

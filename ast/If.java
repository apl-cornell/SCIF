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
        this.orelse = null;
    }

    @Override
    public String genConsVisit(String ctxt, HashMap<String, FuncInfo> funcMap, ArrayList<IfConstraint> cons, LookupMaps varNameMap) {
        String IfNameTest = test.genConsVisit(ctxt, funcMap, cons, varNameMap);
        String IfNamePcBefore = Utils.getLabelNamePc(ctxt);
        ctxt += ".If" + location.toString();
        String IfNamePcAfter = Utils.getLabelNamePc(ctxt);
        cons.add(Utils.genCons(IfNamePcBefore, IfNamePcAfter, location));
        cons.add(Utils.genCons(IfNameTest, IfNamePcAfter, test.location));

        varNameMap.incLayer();
        for (Statement stmt : body) {
            stmt.genConsVisit(ctxt, funcMap, cons, varNameMap);
        }
        varNameMap.decLayer();

        varNameMap.incLayer();
        for (Statement stmt : orelse) {
            stmt.genConsVisit(ctxt, funcMap, cons, varNameMap);
        }
        varNameMap.decLayer();
        return null;
    }
}

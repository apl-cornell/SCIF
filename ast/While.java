package ast;

import utils.*;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;

public class While extends Statement {
    Expression test;
    ArrayList<Statement> body;
    ArrayList<Statement> orelse; //TODO: ignoring for now
    public While(Expression test, ArrayList<Statement> body, ArrayList<Statement> orelse) {
        this.test = test;
        this.body = body;
        this.orelse = orelse;
    }
    public While(Expression test, ArrayList<Statement> body) {
        this.test = test;
        this.body = body;
        this.orelse = null;
    }

    @Override
    public String genConsVisit(String ctxt, HashMap<String, FuncInfo> funcMap, ArrayList<IfConstraint> cons, LookupMaps varNameMap) {
        String IfNameTest = test.genConsVisit(ctxt, funcMap, cons, varNameMap);
        String IfNamePcBefore = Utils.getLabelNamePc(ctxt);
        ctxt += ".While" + location.toString();
        String IfNamePcAfter = Utils.getLabelNamePc(ctxt);
        cons.add(Utils.genCons(IfNamePcAfter, IfNamePcBefore, location));
        cons.add(Utils.genCons(IfNamePcAfter, IfNameTest, test.location));

        varNameMap.incLayer();
        for (Statement stmt : body) {
            stmt.genConsVisit(ctxt, funcMap, cons, varNameMap);
        }
        varNameMap.decLayer();

        return null;
    }
    public void findPrincipal(HashSet<String> principalSet) {
        for (Statement stmt : body) {
            stmt.findPrincipal(principalSet);
        }
        for (Statement stmt : orelse) {
            stmt.findPrincipal(principalSet);
        }
    }

}

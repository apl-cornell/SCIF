package ast;

import utils.*;

import java.security.CodeSigner;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;

public class Program extends Node {
    ArrayList<Statement> body;
    public Program(ArrayList<Statement> body) {
        this.body = body;
    }

    @Override
    public void globalInfoVisit(HashMap<String, VarInfo> varMap, HashMap<String, FuncInfo> funcMap) {
        for (Statement stmt : body) {
            stmt.globalInfoVisit(varMap, funcMap);
        }
    }

    @Override
    public String genConsVisit(String ctxt, HashMap<String, FuncInfo> funcMap, ArrayList<IfConstraint> cons, LookupMaps varNameMap) {
        for (Statement stmt : body) {
            stmt.genConsVisit(ctxt, funcMap, cons, varNameMap);
        }
        return null;
    }

    public void findPrincipal(HashSet<String> principalSet) {
        for (Statement stmt : body) {
            stmt.findPrincipal(principalSet);
        }
    }
}

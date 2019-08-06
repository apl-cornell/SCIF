package ast;

import typecheck.*;

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
    public String genConsVisit(VisitEnv env) {
        for (Statement stmt : body) {
            stmt.genConsVisit(env);
        }
        return null;
    }

    public void findPrincipal(HashSet<String> principalSet) {
        for (Statement stmt : body) {
            stmt.findPrincipal(principalSet);
        }
    }
}

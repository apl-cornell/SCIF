package ast;

import typecheck.*;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;

public class Contract extends Node {
    public String contractName;
    ArrayList<Statement> body;
    public Contract(String contractName, ArrayList<Statement> body) {
        this.contractName = contractName;
        this.body = body;
    }

    @Override
    public void globalInfoVisit(ContractInfo contractInfo) {
        contractInfo.name = contractName;
        for (Statement stmt : body) {
            stmt.globalInfoVisit(contractInfo);
        }
    }

    @Override
    public Context genConsVisit(VisitEnv env) {
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

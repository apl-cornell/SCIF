package ast;

import typecheck.*;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;

public class Contract extends Node {
    public String contractName;
    public ArrayList<TrustConstraint> trustCons;
    ArrayList<Statement> body;
    public Contract(String contractName, ArrayList<TrustConstraint> trustCons, ArrayList<Statement> body) {
        this.contractName = contractName;
        this.trustCons = trustCons;
        this.body = body;
    }

    @Override
    public void globalInfoVisit(ContractInfo contractInfo) {
        contractInfo.name = contractName;
        contractInfo.trustCons = trustCons;
        for (Statement stmt : body) {
            stmt.globalInfoVisit(contractInfo);
        }
    }

    @Override
    public Context genConsVisit(VisitEnv env) {
        //env.prevContext = new Context()
        for (Statement stmt : body) {
            stmt.genConsVisit(env);
        }
        return null;
    }

    public void findPrincipal(HashSet<String> principalSet) {
        for (TrustConstraint trustConstraint : trustCons) {
            trustConstraint.findPrincipal(principalSet);
        }
        for (Statement stmt : body) {
            stmt.findPrincipal(principalSet);
        }
    }
}

package ast;

import typecheck.ContractInfo;

import java.util.ArrayList;
import java.util.HashSet;

public class Interface extends Node {
    String contractName;
    public ArrayList<TrustConstraint> trustCons;
    public ArrayList<FunctionSig> funcSigs;
    public Interface(String contractName, ArrayList<TrustConstraint> trustCons, ArrayList<FunctionSig> funcSigs) {
        this.contractName = contractName;
        this.trustCons = trustCons;
        this.funcSigs = funcSigs;
    }
    @Override
    public void globalInfoVisit(ContractInfo contractInfo) {
        contractInfo.name = contractName;
        contractInfo.trustCons = trustCons;
        for (FunctionSig stmt : funcSigs) {
            stmt.globalInfoVisit(contractInfo);
        }
    }

    public void findPrincipal(HashSet<String> principalSet) {
        for (TrustConstraint trustConstraint : trustCons) {
            trustConstraint.findPrincipal(principalSet);
        }
        for (FunctionSig stmt : funcSigs) {
            stmt.findPrincipal(principalSet);
        }
    }
}

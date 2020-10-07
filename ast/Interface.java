package ast;

import typecheck.ContractSym;
import typecheck.ScopeContext;
import typecheck.NTCEnv;

import java.util.ArrayList;
import java.util.HashSet;

public class Interface extends Node {
    public String contractName;
    public ArrayList<TrustConstraint> trustCons;
    public ArrayList<FunctionSig> funcSigs;
    public Interface(String contractName, ArrayList<TrustConstraint> trustCons, ArrayList<FunctionSig> funcSigs) {
        this.contractName = contractName;
        this.trustCons = trustCons;
        this.funcSigs = funcSigs;
    }

    @Override
    public boolean NTCGlobalInfo(NTCEnv env, ScopeContext parent) {
        ScopeContext now = new ScopeContext(this, parent);
        for (FunctionSig functionSig : funcSigs) {
            if (!functionSig.NTCGlobalInfo(env, now)) return false;
        }
        ContractSym contractSym = new ContractSym(contractName, env.globalSymTab, trustCons, null); //TODO ifl should be included in interface
        env.addSym(contractName, contractSym);
        return true;
    }


    @Override
    public void globalInfoVisit(ContractSym contractSym) {
        contractSym.name = contractName;
        contractSym.trustCons = trustCons;
        for (FunctionSig stmt : funcSigs) {
            stmt.globalInfoVisit(contractSym);
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
    @Override
    public void passScopeContext(ScopeContext parent) {
        scopeContext = new ScopeContext(this, parent);
        for (Node node : children())
            node.passScopeContext(scopeContext);
    }
    @Override
    public ArrayList<Node> children() {
        ArrayList<Node> rtn = new ArrayList<>();
        rtn.addAll(trustCons);
        rtn.addAll(funcSigs);
        return rtn;
    }
}

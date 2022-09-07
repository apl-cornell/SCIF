package ast;

import compile.SolCode;
import typecheck.ContractSym;
import typecheck.ScopeContext;
import typecheck.NTCEnv;

import java.util.ArrayList;
import java.util.HashSet;

public class Interface extends TopLayerNode {

    public String contractName;
    public TrustSetting trustSetting;
    public ArrayList<FunctionSig> funcSigs;

    public Interface(String contractName, TrustSetting trustSetting,
            ArrayList<FunctionSig> funcSigs) {
        this.contractName = contractName;
        this.trustSetting = trustSetting;
        this.funcSigs = funcSigs;
    }

    @Override
    public boolean ntcGlobalInfo(NTCEnv env, ScopeContext parent) {
        ScopeContext now = new ScopeContext(this, parent);
        for (FunctionSig functionSig : funcSigs) {
            if (!functionSig.ntcGlobalInfo(env, now)) {
                return false;
            }
        }
        ContractSym contractSym = new ContractSym(contractName, env.globalSymTab, trustSetting,
                null); //TODO ifl should be included in interface
        env.addSym(contractName, contractSym);
        return true;
    }


    public void globalInfoVisit(ContractSym contractSym) {
        contractSym.name = contractName;
        contractSym.trustSetting = trustSetting;
        for (FunctionSig stmt : funcSigs) {
            stmt.globalInfoVisit(contractSym);
        }
    }

    public void findPrincipal(HashSet<String> principalSet) {
        for (TrustConstraint trustConstraint : trustSetting.trust_list) {
            trustConstraint.findPrincipal(principalSet);
        }
        for (FunctionSig stmt : funcSigs) {
            stmt.findPrincipal(principalSet);
        }
    }

    @Override
    public void passScopeContext(ScopeContext parent) {
        scopeContext = new ScopeContext(this, parent);
        for (Node node : children()) {
            node.passScopeContext(scopeContext);
        }
    }

    @Override
    public ScopeContext ntcGenCons(NTCEnv env, ScopeContext parent) {
        return null;
    }

    @Override
    public void solidityCodeGen(SolCode code) {

    }

    @Override
    public ArrayList<Node> children() {
        ArrayList<Node> rtn = new ArrayList<>();
        rtn.addAll(trustSetting.trust_list);
        rtn.addAll(funcSigs);
        return rtn;
    }
}

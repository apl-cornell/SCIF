package ast;

import compile.SolCode;
import typecheck.ContractSym;
import typecheck.ExpOutcome;
import typecheck.NTCEnv;
import typecheck.ScopeContext;
import typecheck.VisitEnv;

import java.util.HashSet;

public class TrustAtom extends TopLayerNode {

    String name;
    IfLabel ifl;
    boolean isIfl;

    public TrustAtom(String name) {
        this.name = name;
        this.isIfl = false;
    }

    public TrustAtom(IfLabel ifl) {
        this.isIfl = true;
        this.ifl = ifl;
    }

    public String toSherrlocFmt(String contractName) {
        if (isIfl) {
            return ifl.toSherrlocFmt(scopeContext);
        } else {
            return contractName + "." + name;
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
    public void globalInfoVisit(ContractSym contractSym) {

    }

    @Override
    public boolean ntcGlobalInfo(NTCEnv env, ScopeContext parent) {
        return false;
    }

    @Override
    public void findPrincipal(HashSet<String> principalSet) {
        ifl.findPrincipal(principalSet);
    }
}

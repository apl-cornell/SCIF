package ast;

import compile.SolCode;
import java.util.HashSet;
import typecheck.ContractSym;
import typecheck.NTCEnv;
import typecheck.PathOutcome;
import typecheck.ScopeContext;
import typecheck.VisitEnv;

public class Import extends TopLayerNode {

    @Override
    public void globalInfoVisit(ContractSym contractSym) {

    }

    @Override
    public boolean ntcGlobalInfo(NTCEnv env, ScopeContext parent) {
        return false;
    }

    @Override
    public void findPrincipal(HashSet<String> principalSet) {

    }

    @Override
    public ScopeContext ntcGenCons(NTCEnv env, ScopeContext parent) {
        return null;
    }

    @Override
    public void solidityCodeGen(SolCode code) {

    }
    //TODO
}

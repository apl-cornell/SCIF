package ast;

import compile.SolCode;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import typecheck.ContractSym;
import typecheck.NTCEnv;
import typecheck.PathOutcome;
import typecheck.ScopeContext;
import typecheck.VisitEnv;

public class ClassDef extends TopLayerNode {

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
    public List<Node> children() {
        List<Node> rtn = new ArrayList<>();
        return rtn;
    }

//    @Override
//    public void findPrincipal(HashSet<String> principalSet) {
//
//    }
}

package ast;

import compile.SolCode;
import java.util.ArrayList;
import java.util.List;
import typecheck.InterfaceSym;
import typecheck.NTCEnv;
import typecheck.ScopeContext;

public class Import extends TopLayerNode {

    @Override
    public void globalInfoVisit(InterfaceSym contractSym) {

    }

    @Override
    public boolean ntcGlobalInfo(NTCEnv env, ScopeContext parent) {
        return false;
    }

//    @Override
//    public void findPrincipal(HashSet<String> principalSet) {
//
//    }

    @Override
    public ScopeContext ntcGenCons(NTCEnv env, ScopeContext parent) {
        return null;
    }

    @Override
    public void solidityCodeGen(SolCode code) {

    }
    @Override
    public List<Node> children() {
        List<Node> rtn = new ArrayList<>();
        return rtn;
    }
    //TODO
}

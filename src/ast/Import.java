package ast;

import compile.CompileEnv;
import compile.ast.SolNode;
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
    public ScopeContext generateConstraints(NTCEnv env, ScopeContext parent) {
        return null;
    }


    @Override
    public List<Node> children() {
        List<Node> rtn = new ArrayList<>();
        return rtn;
    }
    //TODO
}

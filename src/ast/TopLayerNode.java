package ast;

import typecheck.InterfaceSym;
import typecheck.NTCEnv;
import typecheck.ScopeContext;
import typecheck.exceptions.SemanticException;

public abstract class TopLayerNode extends Node {

    public abstract void globalInfoVisit(InterfaceSym contractSym);

    public abstract boolean ntcGlobalInfo(NTCEnv env, ScopeContext parent) throws SemanticException;

    // public abstract void findPrincipal(HashSet<String> principalSet);
    
}


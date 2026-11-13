package ast;

import typecheck.InterfaceSym;
import typecheck.NTCEnv;
import typecheck.ScopeContext;
import typecheck.exceptions.SemanticException;

public abstract class TopLayerNode extends Node {

    public abstract void globalInfoVisit(InterfaceSym contractSym) throws SemanticException;

    /**
     * Records in {@code env} the symbols this node declares, for regular
     * (non-IFC) type checking.
     *
     * @throws SemanticException if a declaration is ill-formed
     */
    public abstract void ntcGlobalInfo(NTCEnv env, ScopeContext parent) throws SemanticException;

    // public abstract void findPrincipal(HashSet<String> principalSet);
    
}


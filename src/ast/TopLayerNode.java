package ast;

import typecheck.ContractSym;

import java.util.HashSet;
import typecheck.NTCEnv;
import typecheck.ScopeContext;

public abstract class TopLayerNode extends Node {

    public abstract void globalInfoVisit(ContractSym contractSym);

    public abstract boolean ntcGlobalInfo(NTCEnv env, ScopeContext parent);

    // public abstract void findPrincipal(HashSet<String> principalSet);
    
}


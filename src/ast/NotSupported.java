package ast;

import compile.SolCode;
import typecheck.NTCEnv;
import typecheck.ScopeContext;

public class NotSupported extends Node {

    @Override
    public ScopeContext ntcGenCons(NTCEnv env, ScopeContext parent) {
        return null;
    }

    @Override
    public void solidityCodeGen(SolCode code) {

    }
}

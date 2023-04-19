package ast;

import compile.SolCode;
import java.util.ArrayList;
import java.util.List;
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
    @Override
    public List<Node> children() {
        List<Node> rtn = new ArrayList<>();
        return rtn;
    }
}

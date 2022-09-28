package ast;

import compile.SolCode;
import typecheck.NTCEnv;
import typecheck.PathOutcome;
import typecheck.ScopeContext;
import typecheck.VisitEnv;

public class CallStatement extends Statement {

    Call call;

    public CallStatement(Call call) {
        this.call = call;
    }

    @Override
    public ScopeContext ntcGenCons(NTCEnv env, ScopeContext parent) {
        return call.ntcGenCons(env, parent);
    }

    @Override
    public void solidityCodeGen(SolCode code) {
        call.solidityCodeGen(code);
    }

    @Override
    public PathOutcome genConsVisit(VisitEnv env, boolean tail_position) {
        // TODO
        return null;
    }

}

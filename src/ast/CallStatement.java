package ast;

import compile.SolCode;
import java.util.ArrayList;
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
        return call.genConsVisit(env, tail_position).psi;
    }

    @Override
    public ArrayList<Node> children() {
        ArrayList<Node> rtn = new ArrayList<>();
        rtn.add(call);
        return rtn;
    }

}

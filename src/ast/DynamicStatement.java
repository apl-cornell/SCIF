package ast;

import compile.SolCode;
import java.util.ArrayList;
import java.util.List;
import typecheck.*;

public class DynamicStatement extends Statement {

    Call call;

    public DynamicStatement(Call call) {
        this.call = call;
    }

    public ScopeContext ntcGenCons(NTCEnv env, ScopeContext parent) {
        //TODO: check arguments
        return parent;
    }

    @Override
    public void solidityCodeGen(SolCode code) {
        
    }

    public String toSolCode() {
        logger.debug("toSOl: DynamicStatement");
        return call.toSolCode();
    }

    @Override
    public PathOutcome genConsVisit(VisitEnv env, boolean tail_position) {
        return null;
    }

    @Override
    public List<Node> children() {
        List<Node> rtn = new ArrayList<>();
        rtn.add(call);
        return rtn;
    }
}

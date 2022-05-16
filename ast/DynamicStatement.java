package ast;

import compile.SolCode;
import sherrlocUtils.Relation;
import typecheck.*;

public class DynamicStatement extends NonFirstLayerStatement {
    Call call;

    public DynamicStatement(Call call) {
        this.call = call;
    }

    public ScopeContext NTCgenCons(NTCEnv env, ScopeContext parent) {
        //TODO: check arguments
        return parent;
    }

    public String toSolCode() {
        logger.debug("toSOl: DynamicStatement");
        return call.toSolCode();
    }

    @Override
    public PathOutcome genConsVisit(VisitEnv env, boolean tail_position) {
        return null;
    }
}

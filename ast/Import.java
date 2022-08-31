package ast;

import typecheck.ContractSym;
import typecheck.PathOutcome;
import typecheck.VisitEnv;

public class Import extends FirstLayerStatement {
    @Override
    public void globalInfoVisit(ContractSym contractSym) {

    }

    @Override
    public PathOutcome genConsVisit(VisitEnv env, boolean tail_position) {
        return null;
    }
    //TODO
}

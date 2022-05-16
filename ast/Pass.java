package ast;

import typecheck.ContractSym;
import typecheck.PathOutcome;
import typecheck.VisitEnv;

import java.util.HashSet;

public class Pass extends NonFirstLayerStatement {
    public Pass() {
    }
    public void findPrincipal(HashSet<String> principalSet) {
    }

    @Override
    public void globalInfoVisit(ContractSym contractSym) {

    }

    @Override
    public PathOutcome genConsVisit(VisitEnv env, boolean tail_position) {
        return null;
    }
}

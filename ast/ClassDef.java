package ast;

import typecheck.PathOutcome;
import typecheck.VisitEnv;

public class ClassDef extends Statement {

    @Override
    public PathOutcome genConsVisit(VisitEnv env, boolean tail_position) {
        return null;
    }
}

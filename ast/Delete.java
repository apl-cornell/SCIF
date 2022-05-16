package ast;

import typecheck.PathOutcome;
import typecheck.VisitEnv;

import java.util.ArrayList;

public class Delete extends NonFirstLayerStatement {
    ArrayList<Expression> targets;
    public Delete(ArrayList<Expression> targets) {
        this.targets = targets;
    }

    @Override
    public PathOutcome genConsVisit(VisitEnv env, boolean tail_position) {
        return null;
    }
}

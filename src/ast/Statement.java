package ast;

import typecheck.ContractSym;
import typecheck.PathOutcome;
import typecheck.VisitEnv;

public abstract class Statement extends Node {


    public abstract PathOutcome genConsVisit(VisitEnv env, boolean tail_position);
    public String toString() {
        return "";
    }
}

package ast;

import compile.SolCode;
import java.util.List;
import typecheck.NTCEnv;
import typecheck.PathOutcome;
import typecheck.ScopeContext;
import typecheck.VisitEnv;

import java.util.ArrayList;

public class Delete extends Statement {

    List<Expression> targets;

    public Delete(List<Expression> targets) {
        this.targets = targets;
    }

    @Override
    public PathOutcome genConsVisit(VisitEnv env, boolean tail_position) {
        return null;
    }

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
        rtn.addAll(targets);
        return rtn;
    }
}

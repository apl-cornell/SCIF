package ast;

import compile.SolCode;
import java.util.ArrayList;
import java.util.List;
import typecheck.ContractSym;
import typecheck.NTCEnv;
import typecheck.PathOutcome;
import typecheck.ScopeContext;
import typecheck.VisitEnv;

public class Raise extends Statement {

    Expression exception;
    Expression from;

    public Raise(Expression exception, Expression from) {
        this.exception = exception;
        this.from = from;
    }

    public Raise(Expression exception) {
        this.exception = exception;
    }

    public Raise() {
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
        rtn.add(exception);
        rtn.add(from);
        return rtn;
    }
}

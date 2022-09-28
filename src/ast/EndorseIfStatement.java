package ast;

import compile.SolCode;
import java.util.ArrayList;
import java.util.List;
import typecheck.Context;
import typecheck.ExpOutcome;
import typecheck.NTCEnv;
import typecheck.PathOutcome;
import typecheck.ScopeContext;
import typecheck.Utils;
import typecheck.VisitEnv;
import typecheck.sherrlocUtils.Constraint;
import typecheck.sherrlocUtils.Inequality;

public class EndorseStatement extends Statement {

    List<Name> vars;
    IfLabel from, to;

    public EndorseStatement(List<Name> vars, IfLabel from, IfLabel to) {
        this.vars = vars;
        this.from = from;
        this.to = to;
    }

    public ScopeContext ntcGenCons(NTCEnv env, ScopeContext parent) {
        // TODO
        return parent;
    }

    @Override
    public void solidityCodeGen(SolCode code) {

    }

    @Override
    public PathOutcome genConsVisit(VisitEnv env, boolean tail_position) {
        //TODO

        return null;
    }

    @Override
    public ArrayList<Node> children() {
        ArrayList<Node> rtn = new ArrayList<>();
        rtn.addAll(vars);
        rtn.add(from);
        rtn.add(to);
        return rtn;
    }

}

package ast;

import compile.SolCode;
import java.util.ArrayList;
import java.util.List;
import typecheck.ExpOutcome;
import typecheck.NTCEnv;
import typecheck.PathOutcome;
import typecheck.ScopeContext;
import typecheck.VisitEnv;

public class EndorseIfStatement extends Statement {

    List<Name> expressionList;
    IfLabel from, to;
    If ifStatement;

    public EndorseIfStatement(List<Name> expressionList, IfLabel from, IfLabel to,
            If ifStatement) {
        this.expressionList = expressionList;
        this.from = from;
        this.to = to;
        this.ifStatement = ifStatement;
    }

    public ScopeContext ntcGenCons(NTCEnv env, ScopeContext parent) {
        return ifStatement.ntcGenCons(env, parent);
    }

    @Override
    public void solidityCodeGen(SolCode code) {
        // TODO
    }

    @Override
    public PathOutcome genConsVisit(VisitEnv env, boolean tail_position) {
        //TODO

        return null;
    }

    @Override
    public ArrayList<Node> children() {
        ArrayList<Node> rtn = new ArrayList<>();
        rtn.addAll(expressionList);
        rtn.add(from);
        rtn.add(to);
        rtn.add(ifStatement);
        return rtn;
    }

}

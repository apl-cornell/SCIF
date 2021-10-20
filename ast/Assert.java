package ast;

import compile.SolCode;
import typecheck.*;

import java.util.ArrayList;

public class Assert extends NonFirstLayerStatement {
    Expression test;
    Expression msg;
    public Assert(Expression test, Expression msg) {
        this.test = test;
        this.msg = msg;
    }

    @Override
    public Context genConsVisit(VisitEnv env, boolean tail_position) {
        Context tmp = test.genConsVisit(env, tail_position);
        return tmp;
    }

    @Override
    public void SolCodeGen(SolCode code) {
        code.addLine("assert(" + test.toSolCode() + ");");
    }

    @Override
    public ArrayList<Node> children() {
        ArrayList<Node> rtn = new ArrayList<>();
        rtn.add(test);
        rtn.add(msg);
        return rtn;
    }
}

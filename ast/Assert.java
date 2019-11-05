package ast;

import typecheck.*;

import java.util.ArrayList;
import java.util.HashMap;

public class Assert extends Statement {
    Expression test;
    Expression msg;
    public Assert(Expression test, Expression msg) {
        this.test = test;
        this.msg = msg;
    }

    @Override
    public Context genConsVisit(VisitEnv env) {
        Context tmp = test.genConsVisit(env);
        return tmp;
    }
}

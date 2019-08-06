package ast;

import typecheck.FuncInfo;
import typecheck.IfConstraint;
import typecheck.LookupMaps;
import typecheck.VisitEnv;

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
    public String genConsVisit(VisitEnv env) {
        test.genConsVisit(env);
        return null;
    }
}

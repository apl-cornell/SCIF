package ast;

import typecheck.*;

import java.util.ArrayList;
import java.util.HashMap;

public class Attribute extends TrailerExpr {
    Name attr;
    Context ctx;
    public  Attribute(Expression v, Name a, Context c) {
        value = v;
        attr = a;
        ctx = c;
    }
    @Override
    public String genConsVisit(VisitEnv env) {
        String ifNameRnt = value.genConsVisit(env);
        return ifNameRnt;
    }
}

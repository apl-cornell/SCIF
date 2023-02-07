package ast;

import typecheck.ExpOutcome;
import typecheck.ScopeContext;
import typecheck.NTCEnv;
import typecheck.TypeSym;
import typecheck.VisitEnv;

public class Type extends Expression {

    public String getName() {
        return name;
    }

    String name;

    public Type(String name) {
        this.name = name;
    }

    @Override
    public ScopeContext ntcGenCons(NTCEnv env, ScopeContext parent) {
        ScopeContext now = new ScopeContext(this, parent);
        TypeSym typeSym = (TypeSym) env.getCurSym(name);
        env.addCons(now.genEqualCons(typeSym, env, location, "Improper type is specified"));
        return now;
    }

//    public String toSHErrLocFmt() {
//        return "T_" + name + location;
//    }

    // public String toSherrloc(String k, String v) {
    //    return "";
    //}

    public String toSolCode() {
        return name;
    }

    @Override
    public ExpOutcome genConsVisit(VisitEnv env, boolean tail_position) {
        return null;
    }

    @Override
    public boolean typeMatch(Expression expression) {
        return expression instanceof Type &&
                name.equals(((Type) expression).name);
    }

    public boolean isVoid() {
        return name.equals("void");
    }

}

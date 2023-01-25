package ast;

import typecheck.sherrlocUtils.Relation;
import typecheck.*;

public class Name extends Variable {

    public String id;

    //Context ctx;
    public Name(String x) {
        id = x;
        // id = new ScopedName(name, "");
        // ctx = null;
    }
    /*public Name(String name, Context y) {
        id = name;
        ctx = y;
    }*/


    public ScopeContext ntcGenCons(NTCEnv env, ScopeContext parent) {
        Sym s = env.getCurSym(id);
        logger.debug("Name: " + id);
        // logger.debug(s.toString());
        if (s instanceof FuncSym) {
            return null;
        } else if (s instanceof VarSym) {
            ScopeContext now = new ScopeContext(this, parent);
            TypeSym typeSym = ((VarSym) s).typeSym;
            logger.debug(s.getName());
            env.addCons(now.genCons(typeSym.getName(), Relation.EQ, env, location));
            logger.debug(now.toString());
            return now;
        } else if (s instanceof TypeSym) {
            return null;
        }
        return null;
    }

    @Override
    public ExpOutcome genConsVisit(VisitEnv env, boolean tail_position) {
        // assuming the name would be a variable name
        logger.debug("Name: " + id);
        String ifNameRtn = env.getVar(id).labelNameSLC();
        return new ExpOutcome(ifNameRtn, new PathOutcome(new PsiUnit(env.inContext)));
    }

    @Override
    public VarSym getVarInfo(NTCEnv env) {
        return ((VarSym) env.getCurSym(id));
    }

    public VarSym getVarInfo(VisitEnv env, boolean tail_position) {
        VarSym rnt = env.getVar(id);
        return rnt;
    }


    public String toSHErrLocFmt() {
        return id + "." + location;
    }

    @Override
    public String toSolCode() {
        return id;
    }

    @Override
    public boolean typeMatch(Expression expression) {
        return expression instanceof Name &&
                ((Name) expression).id.equals(id);
    }

    public boolean typeMatch(Name value) {
        return id.equals(value.id);
    }
}

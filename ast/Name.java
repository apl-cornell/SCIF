package ast;

import sherrlocUtils.Relation;
import typecheck.*;

public class Name extends Variable {
    public String id;
    //Context ctx;
    public Name(String x) {
        id = x;
        // ctx = null;
    }
    /*public Name(String x, Context y) {
        id = x;
        ctx = y;
    }*/


    public ScopeContext NTCgenCons(NTCEnv env, ScopeContext parent) {
        Sym s = env.getCurSym(id);
        logger.debug("Name: " + id);
        // logger.debug(s.toString());
        if (s instanceof FuncSym) {
            return null;
        } else if (s instanceof VarSym) {
            ScopeContext now = new ScopeContext(this, parent);
            TypeSym typeSym = ((VarSym) s).typeSym;
            logger.debug(s.name);
            env.addCons(now.genCons(typeSym.name, Relation.EQ, env, location));
            logger.debug(now.toString());
            return now;
        } else if (s instanceof TypeSym) {
            return null;
        }
        return null;
    }

    @Override
    public VarSym getVarInfo(NTCEnv env) {
        return ((VarSym) env.getCurSym(id));
    }

    @Override
    public Context genConsVisit(VisitEnv env) {
        // assuming the name would be a variable name
        String ifNameRnt = env.getVar(id).labelToSherrlocFmt();
        return new Context(ifNameRnt, env.prevContext.lockName);
    }

    public VarSym getVarInfo(VisitEnv env) {
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

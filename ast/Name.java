package ast;

import sherrlocUtils.Relation;
import typecheck.*;

import java.util.ArrayList;
import java.util.HashMap;

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


    public NTCContext NTCgenCons(NTCEnv env, NTCContext parent) {
        Sym s = env.getCurSym(id);
        if (s instanceof FuncSym) {
            return null;
        } else if (s instanceof VarSym) {
            NTCContext now = new NTCContext(this, parent);
            TypeInfo typeInfo = ((VarSym) s).varInfo.typeInfo;
            env.addCons(now.genCons(typeInfo.type.typeName, Relation.EQ, env, location));
            return now;
        } else if (s instanceof TypeSym) {
            return null;
        }
        return null;
    }

    @Override
    public Context genConsVisit(VisitEnv env) {
        // assuming the name would be a variable name
        String ifNameRnt = env.varNameMap.getInfo(id).labelToSherrlocFmt();
        return new Context(ifNameRnt, env.prevContext.lockName);
    }

    public VarInfo getVarInfo(VisitEnv env) {
        VarInfo rnt = env.varNameMap.getInfo(id);
        return rnt;
    }


    public String toSHErrLocFmt() {
        return id + "." + location;
    }

    @Override
    public String toSolCode() {
        return id;
    }
}

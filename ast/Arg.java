package ast;

import sherrlocUtils.Relation;
import typecheck.*;

import java.util.ArrayList;
import java.util.HashSet;

public class Arg extends Node {
    String name;
    Type annotation;
    public Arg(String name, Type annotation) {
        this.name = name;
        this.annotation = annotation;
    }

    public VarSym parseArg(ContractSym contractSym) {
        return contractSym.toVarSym(name, annotation, false, location, scopeContext);
    }

    public VarSym parseArg(NTCEnv env, ScopeContext parent) {
        ScopeContext now = new ScopeContext(this, parent);
        return env.toVarSym(name, annotation, false, location, now);
    }

    @Override
    public ScopeContext NTCgenCons(NTCEnv env, ScopeContext parent) {
        ScopeContext now = new ScopeContext(this, parent);

        ScopeContext type = annotation.NTCgenCons(env, now);

        env.addSym(name, env.toVarSym(name, annotation, false, location, now));

        env.cons.add(type.genCons(now, Relation.EQ, env, location));

        return null;
    }

    @Override
    public Context genConsVisit(VisitEnv env) {

        if (annotation instanceof LabeledType) {
            if (annotation instanceof DepMap) {
                ((DepMap) annotation).findPrincipal(env.principalSet);
            } else {
                ((LabeledType) annotation).ifl.findPrincipal(env.principalSet);
            }
        }
        // String ifName = env.ctxt + "." + name;
        VarSym varSym = env.curContractSym.toVarSym(name, annotation, false, location, scopeContext);
        env.addVar(name, varSym);
        // env.varNameMap.add(name, ifName, varSym);

        if (varSym.typeSym.name.equals(Utils.ADDRESSTYPE)) {
            env.principalSet.add(varSym.toSherrlocFmt());
        }

        return null;
    }
    public void findPrincipal(HashSet<String> principalSet) {
        if (annotation instanceof LabeledType) {
            ((LabeledType) annotation).ifl.findPrincipal(principalSet);
        }
    }


    public String toSolCode() {
        return annotation.toSolCode() + " " + name;
    }

    @Override
    public ArrayList<Node> children() {
        ArrayList<Node> rtn = new ArrayList<>();
        rtn.add(annotation);
        return rtn;
    }
}

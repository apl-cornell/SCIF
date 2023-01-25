package ast;

import compile.SolCode;
import java.util.List;
import typecheck.sherrlocUtils.Relation;
import typecheck.*;

import java.util.ArrayList;
import java.util.HashSet;

public class Arg extends Node {

    String name;
    LabeledType annotation;
    boolean isStatic;
    boolean isFinal;


    public Arg(String name, LabeledType annotation) {
        this.name = name;
        this.annotation = annotation;
        this.isStatic = false;
        this.isFinal = false;
    }
    public Arg(String name, LabeledType annotation, boolean isStatic, boolean isFinal) {
        this.name = name;
        this.annotation = annotation;
        this.isStatic = isStatic;
        this.isFinal = isFinal;
    }

    public void setToDefault(IfLabel ifl) {
        annotation.setToDefault(ifl);
    }

    public VarSym parseArg(ContractSym contractSym) {
        return contractSym.toVarSym(name, annotation, false, false, location, scopeContext);
    }

    public VarSym parseArg(NTCEnv env, ScopeContext parent) {
        // ScopeContext now = new ScopeContext(this, parent);
        return env.toVarSym(name, annotation, false, false, location, parent);
    }

    @Override
    public ScopeContext ntcGenCons(NTCEnv env, ScopeContext parent) {
        // System.err.println(scopeContext);
        ScopeContext now = new ScopeContext(this, parent);

        ScopeContext type = annotation.ntcGenCons(env, now);

        env.addSym(name, env.toVarSym(name, annotation, false, false, location, now));

        env.addCons(type.genCons(now, Relation.EQ, env, location));

        return now;
    }

    @Override
    public void solidityCodeGen(SolCode code) {
        
    }

    public void genConsVisit(VisitEnv env, boolean tail_position) {

//        if (annotation instanceof LabeledType) {
//            if (annotation instanceof DepMap) {
//                ((DepMap) annotation).findPrincipal(env.principalSet);
//            } else {
//                ((LabeledType) annotation).ifl.findPrincipal(env.principalSet);
//            }
//        }
        // String ifName = env.ctxt + "." + name;
        VarSym varSym = env.curContractSym.toVarSym(name, annotation, false, false, location,
                scopeContext);
        env.addVar(name, varSym);
        // env.varNameMap.add(name, ifName, varSym);

        if (varSym.typeSym.getName().equals(Utils.ADDRESSTYPE)) {
            env.principalSet().add(varSym);
        }

    }

//    public void findPrincipal(HashSet<String> principalSet) {
//        if (annotation instanceof LabeledType) {
//            ((LabeledType) annotation).ifl.findPrincipal(principalSet);
//        }
//    }


    public String toSolCode() {
        return annotation.toSolCode() + " " + name;
    }

    @Override
    public List<Node> children() {
        ArrayList<Node> rtn = new ArrayList<>();
        rtn.add(annotation);
        return rtn;
    }

    public boolean typeMatch(Arg arg) {
        return name.equals(arg.name) && annotation.typeMatch(arg.annotation);
    }
}

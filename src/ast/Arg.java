package ast;

import compile.SolCode;
import java.util.List;
import typecheck.sherrlocUtils.Relation;
import typecheck.*;

import java.util.ArrayList;
import java.util.HashSet;

public class Arg extends Node {

    private String name;
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
        VarSym varSym = contractSym.newVarSym(name, annotation, isStatic, isFinal, true, location, scopeContext);
        contractSym.symTab.add(name, varSym);
        return varSym;
    }

    public VarSym parseArg(InterfaceSym interfaceSym) {
        VarSym varSym = interfaceSym.newVarSym(name, annotation, isStatic, isFinal, true, location, scopeContext);
        interfaceSym.symTab.add(name, varSym);
        return varSym;
    }

    public VarSym parseArg(NTCEnv env, ScopeContext parent) {
        // ScopeContext now = new ScopeContext(this, parent);
        VarSym varSym = env.newVarSym(name, annotation, isStatic, isFinal, true, location, parent);
        env.addSym(name, varSym);
        return varSym;
    }

    @Override
    public ScopeContext ntcGenCons(NTCEnv env, ScopeContext parent) {
        // System.err.println(scopeContext);
        ScopeContext now = new ScopeContext(this, parent);

        ScopeContext type = annotation.ntcGenCons(env, now);

        env.addSym(name, env.newVarSym(name, annotation, isStatic, isFinal, true, location, now));

        env.addCons(type.genCons(now, Relation.EQ, env, location));

        return now;
    }

    @Override
    public void solidityCodeGen(SolCode code) {
        
    }

    public void genConsVisit(VisitEnv env, boolean tail_position) {

        VarSym varSym = env.curContractSym().newVarSym(name, annotation, isStatic, isFinal, true, location,
                scopeContext);
        env.addVar(name, varSym);

        if (varSym.isFinal && varSym.typeSym.getName().equals(Utils.ADDRESS_TYPE)) {
            env.addPrincipal(varSym);
        }

    }

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

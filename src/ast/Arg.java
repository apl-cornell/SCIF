package ast;

import compile.CompileEnv;
import compile.ast.Argument;
import compile.ast.Type;
import java.util.List;

import typecheck.exceptions.SemanticException;
import typecheck.sherrlocUtils.Relation;
import typecheck.*;

import java.util.ArrayList;

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

    public VarSym parseArg(InterfaceSym interfaceSym) throws SemanticException {
        VarSym varSym = interfaceSym.newVarSym(name, annotation, isStatic, isFinal, true, location, scopeContext);
        try {
            interfaceSym.symTab.add(name, varSym);
        } catch (SymTab.AlreadyDefined e) {
            throw new RuntimeException(e); // can't happen?
        }
        if (annotation.label() != null) {
            varSym.setLabel(interfaceSym.newLabel(annotation.label()));
        }
        return varSym;
    }

    public VarSym parseArg(NTCEnv env, ScopeContext parent)
            throws SemanticException
    {
        // ScopeContext now = new ScopeContext(this, parent);
        VarSym varSym = env.newVarSym(name, annotation, isStatic, isFinal, true, location, parent);
        annotation.type().setContractType(varSym.typeSym instanceof InterfaceSym);
        try {
            env.addSym(name, varSym);
        } catch (SymTab.AlreadyDefined e) {
            throw new SemanticException(e.id, location);
        }
        if (annotation.label() != null) {
            varSym.setLabel(env.newLabel(annotation.label()));
        }
        return varSym;
    }

    @Override
    public ScopeContext generateConstraints(NTCEnv env, ScopeContext parent) throws SemanticException {
        ScopeContext now = new ScopeContext(this, parent);
        VarSym varSym = env.newVarSym(name, annotation, isStatic, isFinal, true, location, now);
        try {
            env.addSym(name, varSym);
        } catch (SymTab.AlreadyDefined e) {
            throw new RuntimeException(e); // can't happen?
        }
        if (annotation.label() != null) {
            varSym.setLabel(env.newLabel(annotation.label()));
        }
        ScopeContext type = annotation.generateConstraints(env, now);

        env.addCons(type.genCons(now, Relation.EQ, env, location));

        return now;
    }

    public Argument solidityCodeGen(CompileEnv code) {
        Type varType = annotation.type().solidityCodeGen(code);
        code.addLocalVar(name, varType);
        return new Argument(varType, name);
    }

    public void genConsVisit(VisitEnv env, boolean tail_position) throws SemanticException {

        VarSym varSym = env.curContractSym().newVarSym(name, annotation, isStatic, isFinal, true, location,
                scopeContext);
        try {
            env.addVar(name, varSym);
        } catch (SymTab.AlreadyDefined e) {
            throw new SemanticException("Already defined: " + name, location);
        }
        if (annotation.label() != null) {
            varSym.setLabel(env.toLabel(annotation.label()));
        }

        if (varSym.isFinal && varSym.typeSym.getName().equals(Utils.ADDRESS_TYPE)) {
            env.addPrincipal(varSym);
        }
    }

    public String toSolCode() {
        return annotation.toSolCode() + (annotation.type().isPrimitive() ? " " : " memory ") + name;
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

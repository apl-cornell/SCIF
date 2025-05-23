package ast;

import compile.CompileEnv;
import compile.ast.ContractType;
import compile.ast.PrimitiveType;
import compile.ast.StructType;
import java.util.ArrayList;
import java.util.List;
import typecheck.ExpOutcome;
import typecheck.InterfaceSym;
import typecheck.ScopeContext;
import typecheck.NTCEnv;
import typecheck.StructTypeSym;
import typecheck.TypeSym;
import typecheck.Utils;
import typecheck.VisitEnv;
import typecheck.exceptions.SemanticException;

public class Type extends Node {

    public String name() {
        return name;
    }

    String name;
    boolean isContractType;
    boolean isStructType;

    public Type(String name) {
        this.name = name;
    }

    @Override
    public ScopeContext genTypeConstraints(NTCEnv env, ScopeContext parent) throws SemanticException {
        ScopeContext now = new ScopeContext(this, parent);
        TypeSym typeSym = (TypeSym) env.getCurSym(name);
        isContractType = typeSym instanceof InterfaceSym;
        isStructType = typeSym instanceof StructTypeSym;
        // System.err.println(name + " is contract: " + isContractType);
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

    public ExpOutcome genConsVisit(VisitEnv env, boolean tail_position) {
        assert false;
        return null;
    }

    public boolean typeMatch(Type expression) {
        return expression instanceof Type &&
                name.equals(((Type) expression).name);
    }

    public boolean isVoid() {
        return name.equals(Utils.VOID_TYPE);
    }

    @Override
    public List<Node> children() {
        List<Node> rtn = new ArrayList<>();
        return rtn;
    }

    public boolean isPrimitive() {
        return true;
    }

    public void setToDefault(IfLabel ifl) {
        // normal types don't carry any labels
    }

    public compile.ast.Type solidityCodeGen(CompileEnv code) {
        return isContractType ? new ContractType(name) :
                (isStructType ? new StructType(name) :
                new PrimitiveType(name));
    }

    public void setContractType(boolean b) {
        isContractType = b;
    }
}

package ast;

import compile.CompileEnv;
import compile.ast.ContractType;
import compile.ast.PrimitiveType;
import java.util.ArrayList;
import java.util.List;
import typecheck.ExpOutcome;
import typecheck.InterfaceSym;
import typecheck.NTCEnv;
import typecheck.ScopeContext;
import typecheck.TypeSym;
import typecheck.Utils;
import typecheck.VisitEnv;

public class ExtType extends Type {

    String contractName;

    public ExtType(String contractName, String name) {
        super(name);
        this.contractName = contractName;
    }

    @Override
    public ScopeContext generateConstraints(NTCEnv env, ScopeContext parent) {
        ScopeContext now = new ScopeContext(this, parent);
        TypeSym typeSym = (TypeSym) env.getExtSym(contractName, name);
        assert !(typeSym instanceof InterfaceSym);
        isContractType = false;
        env.addCons(now.genEqualCons(typeSym, env, location, "Improper type is specified"));
        return now;
    }

    public String toSolCode() {
        return contractName + "." + name;
    }

    public ExpOutcome genConsVisit(VisitEnv env, boolean tail_position) {
        assert false;
        return null;
    }

    public boolean typeMatch(Type expression) {
        return expression instanceof ExtType &&
                name.equals(((Type) expression).name);
    }

    public boolean isVoid() {
        return false;
    }

    @Override
    public List<Node> children() {
        List<Node> rtn = new ArrayList<>();
        return rtn;
    }

    public boolean isPrimitive() {
        return false;
    }

    public void setToDefault(IfLabel ifl) {
        // normal types don't carry any labels
    }

    public compile.ast.Type solidityCodeGen(CompileEnv code) {
        return new compile.ast.ExtType(contractName, name);
    }

    public String contractName() {
        return contractName;
    }
}

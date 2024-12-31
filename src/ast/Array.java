package ast;

import compile.CompileEnv;
import compile.ast.ArrayType;
import compile.ast.FixedArrayType;

import java.util.ArrayList;
import typecheck.ArrayTypeSym;
import typecheck.NTCEnv;
import typecheck.ScopeContext;
import typecheck.Utils;

public class Array extends Type {

    public int size;
    public Type valueType;

    public Array(int size, Type valueType) {
        super(Utils.ARRAY_TYPE);
        this.size = size;
        this.valueType = valueType;
    }
    @Override
    public String toSolCode() {
        String rtn = valueType.toSolCode();
        // because arrays in Solidity are defined in the opposite index dimension order
        rtn += "[" + (size == 0 ? "" : size) + "]";
        return rtn;
    }

    @Override
    public ArrayList<Node> children() {
        ArrayList<Node> rtn = new ArrayList<>();
        rtn.add(valueType);
        return rtn;
    }

    @Override
    public boolean typeMatch(Type annotation) {
        return annotation instanceof Array &&
                super.typeMatch(annotation) &&
                size == (((Array) annotation).size) &&
                valueType.typeMatch(((Array) annotation).valueType);
    }

    @Override
    public ScopeContext generateConstraints(NTCEnv env, ScopeContext parent) {
        ScopeContext now = new ScopeContext(this, parent);
        valueType.generateConstraints(env, parent);
        ArrayTypeSym typeSym = (ArrayTypeSym) env.toTypeSym(this, scopeContext);
        assert typeSym != null : name;
        env.addCons(now.genEqualCons(typeSym, env, location, "Improper type is specified"));
        return now;
    }

    @Override
    public boolean isPrimitive() {
        return false;
    }
    @Override
    public compile.ast.Type solidityCodeGen(CompileEnv code) {
        if (size == 0) {
            return new ArrayType(valueType.solidityCodeGen(code));
        } else {
            return new FixedArrayType(valueType.solidityCodeGen(code), size);
        }
    }
}

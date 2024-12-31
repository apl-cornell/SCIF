package ast;

import compile.CompileEnv;
import compile.ast.MapType;

import java.util.ArrayList;
import java.util.List;
import typecheck.MapTypeSym;
import typecheck.NTCEnv;
import typecheck.ScopeContext;
import typecheck.Utils;

public class Map extends Type {

    public Type keyType;
    public Type valueType;
    protected IfLabel valueLabel;

    public Map(Type keyType, Type valueType) {
        super(Utils.MAP_TYPE);
        this.keyType = keyType;
        this.valueType = valueType;
    }
    public Map(Type keyType, Type valueType, IfLabel valueLabel) {
        super(Utils.MAP_TYPE);
        this.keyType = keyType;
        this.valueType = valueType;
        this.valueLabel = valueLabel;
    }
    @Override
    public String toSolCode() {
        String rtn = "mapping";
        String k = keyType.toSolCode(), v = valueType.toSolCode();
        rtn += "(" + k + " => " + v + ")";
        return rtn;
    }

    @Override
    public List<Node> children() {
        ArrayList<Node> rtn = new ArrayList<>();
        rtn.add(keyType);
        rtn.add(valueType);
        rtn.add(valueLabel);
        return rtn;
    }

    @Override
    public boolean typeMatch(Type annotation) {
        return annotation instanceof Map &&
                super.typeMatch(annotation) &&
                keyType.typeMatch(((Map) annotation).keyType) &&
                valueType.typeMatch(((Map) annotation).valueType) &&
                valueLabel.typeMatch(((Map) annotation).valueLabel)
                ;
    }

    @Override
    public ScopeContext generateConstraints(NTCEnv env, ScopeContext parent) {
        ScopeContext now = new ScopeContext(this, parent);
        keyType.generateConstraints(env, parent);
        valueType.generateConstraints(env, parent);
        MapTypeSym typeSym = (MapTypeSym) env.toTypeSym(this, scopeContext);
        assert typeSym != null : name;
        env.addCons(now.genEqualCons(typeSym, env, location, "Improper type is specified"));
        return now;
    }

    @Override
    public boolean isPrimitive() {
        return false;
    }

    @Override
    public void setToDefault(IfLabel ifl) {
        valueType.setToDefault(ifl);
    }

    @Override
    public compile.ast.Type solidityCodeGen(CompileEnv code) {
        return new MapType(keyType.solidityCodeGen(code), valueType.solidityCodeGen(code));
    }
}

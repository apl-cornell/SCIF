package ast;

import java.util.List;
import typecheck.DepMapTypeSym;
import typecheck.NTCEnv;
import typecheck.ScopeContext;
import typecheck.TypeSym;
import typecheck.Utils;
import typecheck.VarSym;

public class DepMap extends Map {
    final private String keyName;
    final private LabeledType labeledKeyType;
//    private IfLabel valueLabel;
    public DepMap(Type keyType, String keyName, LabeledType valueType) {
        super(keyType, valueType.type(), valueType.label());
        this.keyName = keyName;
        this.labeledKeyType = new LabeledType(keyType, new PrimitiveIfLabel(new Name(Utils.LABEL_BOTTOM)));
//        this.valueLabel = valueType.label();
    }

    @Override
    public boolean typeMatch(Type annotation) {
        return annotation instanceof DepMap &&
                super.typeMatch(annotation) &&
                keyName.equals(((DepMap) annotation).keyName)
        ;
//        &&
//                valueLabel.typeMatch(((DepMap) annotation).valueLabel);
    }

    @Override
    public List<Node> children() {
        List<Node> rtn = super.children();
//        rtn.add(valueLabel);
        return rtn;
    }

    @Override
    public ScopeContext generateConstraints(NTCEnv env, ScopeContext parent) {
        ScopeContext now = new ScopeContext(this, parent);
        keyType.generateConstraints(env, parent);

        TypeSym keyTypeSym = env.toTypeSym(keyType, now);
        VarSym keyVarSym = new VarSym(
                keyName,
                keyTypeSym,
                null,
                keyType.location,
                now,
                false,
                true,
                true
        );
        env.enterNewScope();
        env.addSym(keyName, keyVarSym);
        valueType.generateConstraints(env, now);

        env.exitNewScope();
        DepMapTypeSym typeSym = (DepMapTypeSym) env.toTypeSym(this, scopeContext);
        assert typeSym != null : name;
        env.addCons(now.genEqualCons(typeSym, env, location, "Improper type is specified"));
        return now;
    }

    public String keyName() {
        return keyName;
    }

    public LabeledType labeledKeyType() {
        return labeledKeyType;
    }

    public IfLabel valueLabel() {
        return valueLabel;
    }

    @Override
    public void setToDefault(IfLabel ifl) {
        if (valueLabel == null) {
            valueLabel = ifl;
        }
        valueType.setToDefault(valueLabel);
    }
}

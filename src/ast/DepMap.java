package ast;

import java.util.ArrayList;
import typecheck.MapTypeSym;
import typecheck.NTCEnv;
import typecheck.ScopeContext;

public class DepMap extends Map {
    final private String keyName;
    final private IfLabel valueLabel;
    public DepMap(Type keyType, String keyName, LabeledType valueType) {
        super(keyType, valueType.type());
        this.keyName = keyName;
        this.valueLabel = valueType.label();
    }

    @Override
    public boolean typeMatch(Expression annotation) {
        return annotation instanceof DepMap &&
                super.typeMatch(annotation) &&
                keyName.equals(((DepMap) annotation).keyName) &&
                valueLabel.typeMatch(((DepMap) annotation).valueLabel);
    }

    @Override
    public ArrayList<Node> children() {
        ArrayList<Node> rtn = super.children();
        rtn.add(valueLabel);
        return rtn;
    }

    @Override
    public ScopeContext ntcGenCons(NTCEnv env, ScopeContext parent) {
        assert false;
        return null;
    }
}

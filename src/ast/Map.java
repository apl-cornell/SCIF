package ast;

import java.awt.*;
import java.util.ArrayList;
import typecheck.Utils;

public class Map extends LabeledType {

    public Type keyType;
    public Type valueType;

    public Map(Type keyType, Type valueType, IfLabel ifl) {
        super(Utils.MAP_TYPE, ifl);
        this.keyType = keyType;
        this.valueType = valueType;
    }
    @Override
    public String toSolCode() {
        String rtn = "mapping";
        String k = keyType.toSolCode(), v = valueType.toSolCode();
        rtn += "(" + k + " => " + v + ")";
        return rtn;
    }

    @Override
    public ArrayList<Node> children() {
        ArrayList<Node> rtn = new ArrayList<>();
        rtn.add(keyType);
        rtn.add(valueType);
        return rtn;
    }

    @Override
    public boolean typeMatch(Type annotation) {
        return annotation instanceof Map &&
                super.typeMatch(annotation) &&
                keyType.typeMatch(((Map) annotation).keyType) &&
                valueType.typeMatch(((Map) annotation).valueType);
    }
}

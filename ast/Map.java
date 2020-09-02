package ast;

import java.awt.*;
import java.util.ArrayList;

public class Map extends LabeledType {
    public Type keyType;
    public Type valueType;
    public Map(Type keyType, Type valueType, IfLabel ifl) {
        super("map", ifl);
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
}

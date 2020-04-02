package ast;

import java.awt.*;

public class Map extends LabeledType {
    public LabeledType keyType;
    public LabeledType valueType;
    public Map(LabeledType keyType, LabeledType valueType, IfLabel ifl) {
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
}

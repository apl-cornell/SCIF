package ast;

import java.awt.*;

public class Map extends LabeledType {
    LabeledType keyType;
    LabeledType valueType;
    public Map(LabeledType keyType, LabeledType valueType, IfLabel ifl) {
        super(null, ifl);
        this.keyType = keyType;
        this.valueType = valueType;
    }
}

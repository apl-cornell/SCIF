package ast;

import utils.CodeLocation;

public class DepMap extends LabeledType {
    public LabeledType keyType;
    public LabeledType valueType;
    public DepMap(Name keyName, LabeledType keyType, LabeledType valueType, IfLabel ifl) {
        super(keyName, ifl);
        this.keyType = keyType;
        this.valueType = valueType;
    }

    public String toSherrloc(String name, String value) {
        return ifl.toSherrlocFmt(name, value);
    }
}

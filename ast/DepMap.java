package ast;

import java.util.HashSet;

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

    @Override
    public void findPrincipal(HashSet<String> principalSet) {
        ifl.findPrincipal(principalSet);
        keyType.findPrincipal(principalSet, x.id);
        valueType.findPrincipal(principalSet, x.id);
    }
}

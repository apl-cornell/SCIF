package ast;

import java.util.HashSet;

public class DepMap extends LabeledType {
    public LabeledType keyType;
    public LabeledType valueType;
    public DepMap(String keyName, LabeledType keyType, LabeledType valueType, IfLabel ifl) {
        super(keyName, ifl);
        this.keyType = keyType;
        this.valueType = valueType;
    }

//    public String toSherrloc(String name, String value) {
//        return ifl.toSHErrLocFmt(name, value);
//    }

//    @Override
//    public void findPrincipal(HashSet<String> principalSet) {
//        ifl.findPrincipal(principalSet);
//        keyType.findPrincipal(principalSet, name);
//        valueType.findPrincipal(principalSet, name);
//    }

    @Override
    public boolean typeMatch(Type annotation) {
        return annotation instanceof Map &&
                super.typeMatch(annotation) &&
                keyType.typeMatch(((Map) annotation).keyType) &&
                valueType.typeMatch(((Map) annotation).valueType);
    }
}

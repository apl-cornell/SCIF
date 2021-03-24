package typecheck;

import ast.IfLabel;

public class DepMapTypeSym extends MapTypeSym {
    public TypeSym keyType;
    public TypeSym valueType;

    public DepMapTypeSym(TypeSym keyType, TypeSym valueType) {
        super(keyType, valueType);
    }

    public DepMapTypeSym(DepMapTypeSym depMapTypeInfo) {
        name = depMapTypeInfo.name;
        keyType = depMapTypeInfo.keyType;
        valueType = depMapTypeInfo.valueType;
    }
}

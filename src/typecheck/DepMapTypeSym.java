package typecheck;

import ast.IfLabel;

public class DepMapTypeSym extends MapTypeSym {

    public TypeSym keyType;
    public TypeSym valueType;

    public DepMapTypeSym(TypeSym keyType, TypeSym valueType, ScopeContext defContext) {
        super(keyType, valueType, defContext);
    }

    public DepMapTypeSym(DepMapTypeSym depMapTypeInfo) {
        super(depMapTypeInfo);
        keyType = depMapTypeInfo.keyType;
        valueType = depMapTypeInfo.valueType;
    }
}

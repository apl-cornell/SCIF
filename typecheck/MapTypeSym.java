package typecheck;

import ast.IfLabel;

public class MapTypeSym extends TypeSym {
    // types of key and value have no labels
    public TypeSym keyType;
    public TypeSym valueType;

    public MapTypeSym() {
    }

    public MapTypeSym(TypeSym keyType, TypeSym valueType) {
        this.keyType = keyType;
        this.valueType = valueType;
        this.name = getName();
    }

    public MapTypeSym(MapTypeSym depMapTypeInfo) {
        keyType = depMapTypeInfo.keyType;
        valueType = depMapTypeInfo.valueType;
        name = getName();
    }

    @Override
    public String getName() {
        return "Map." + keyType.getName() + "." + valueType.getName();
    }
}

package typecheck;

import ast.IfLabel;
import ast.Type;

public class MapTypeSym extends TypeSym {
    // types of key and value have no labels
    public TypeSym keyType;
    public TypeSym valueType;

    /*public MapTypeSym() {
    }*/

    public MapTypeSym(TypeSym keyType, TypeSym valueType, ScopeContext defContext) {
        super(getMapName(keyType, valueType), defContext);
        this.keyType = keyType;
        this.valueType = valueType;
        // this.name = getName();
    }

    public MapTypeSym(MapTypeSym depMapTypeInfo) {
        super(depMapTypeInfo.getName(), depMapTypeInfo.defContext());
        keyType = depMapTypeInfo.keyType;
        valueType = depMapTypeInfo.valueType;
        // name = getName();
    }

//    @Override
//    public String getName() {
//        return this.name;
//        // return "Map." + keyType.getName() + "." + valueType.getName();
//    }

    public static String getMapName(TypeSym keyType, TypeSym valueType) {
        return "Map." + keyType.getName() + "." + valueType.getName();
    }
}

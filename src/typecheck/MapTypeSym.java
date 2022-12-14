package typecheck;

import ast.IfLabel;
import ast.Type;

public class MapTypeSym extends TypeSym {
    // types of key and value have no labels
    public TypeSym keyType;
    public TypeSym valueType;

    /*public MapTypeSym() {
    }*/

    public MapTypeSym(TypeSym keyType, TypeSym valueType) {
        super(getMapName(keyType, valueType));
        this.keyType = keyType;
        this.valueType = valueType;
        // this.name = getName();
    }

    public MapTypeSym(MapTypeSym depMapTypeInfo) {
        super(depMapTypeInfo.getName());
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

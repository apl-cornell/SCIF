package typecheck;

import ast.IfLabel;

public class MapTypeInfo extends TypeInfo {
    public TypeInfo keyType;
    public TypeInfo valueType;

    public MapTypeInfo(Type typeName, IfLabel ifl, boolean isConst, TypeInfo keyType, TypeInfo valueType) {
        super(typeName, ifl, isConst);
        this.keyType = keyType;
        this.valueType = valueType;
    }

    public MapTypeInfo(MapTypeInfo depMapTypeInfo) {
        super(depMapTypeInfo);
        keyType = new TypeInfo(depMapTypeInfo.keyType);
        valueType = new TypeInfo(depMapTypeInfo.valueType);
    }

    @Override
    public void replace(String k, String v) {
        super.replace(k, v);
        keyType.replace(k, v);
        valueType.replace(k, v);
    }
}

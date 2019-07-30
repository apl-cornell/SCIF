package utils;

import ast.IfLabel;

public class DepMapTypeInfo extends TypeInfo {
    public TypeInfo keyType;
    public TypeInfo valueType;

    public DepMapTypeInfo(String typeName, IfLabel ifl, boolean isConst, TypeInfo keyType, TypeInfo valueType) {
        super(typeName, ifl, isConst);
        this.keyType = keyType;
        this.valueType = valueType;
    }

    public DepMapTypeInfo(DepMapTypeInfo depMapTypeInfo) {
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

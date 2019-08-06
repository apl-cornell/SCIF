package typecheck;

import ast.IfLabel;

public class TypeInfo {
    public String typeName;
    public IfLabel ifl;
    public boolean isConst;

    public TypeInfo(String typeName, IfLabel ifl, boolean isConst) {
        this.typeName = typeName;
        this.ifl = ifl;
        this.isConst = isConst;
    }

    public TypeInfo(TypeInfo typeInfo) {
        typeName = typeInfo.typeName;
        ifl = typeInfo.ifl;
        isConst = typeInfo.isConst;
    }

    public String getLabel() {
        if (ifl != null) {
            return ifl.toSherrlocFmt();
        } else {
            return null;
        }
    }

    public void replace(String k, String v) {
        ifl.replace(k, v);
    }
}

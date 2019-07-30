package utils;

import ast.IfLabel;
import ast.Name;
import com.owlike.genson.Genson;
import com.owlike.genson.GensonBuilder;
import utils.CodeLocation;

public class VarInfo {
    public String varName;
    public TypeInfo type;

    public CodeLocation location;

    public VarInfo(String varName, TypeInfo type, CodeLocation location) {
        this.varName = varName;
        this.type = type;
        this.location = location;
    }

    public String getLabel() {
        return type.getLabel();
    }

    static Genson genson = new GensonBuilder().useClassMetadata(true).useIndentation(true).useRuntimeType(true).create();
    @Override
    public String toString() {
        return genson.serialize(this);
    }

    public String toSherrlocFmt() {
        return varName;// + ".." + "lblVar";
    }
}

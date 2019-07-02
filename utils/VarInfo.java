package utils;

import ast.IfLabel;
import com.owlike.genson.Genson;
import com.owlike.genson.GensonBuilder;
import utils.CodeLocation;

public class VarInfo {
    public String varName;
    public IfLabel ifLabel;

    public CodeLocation location;

    public VarInfo(String varName, IfLabel ifLabel, CodeLocation location) {
        this.varName = varName;
        this.ifLabel = ifLabel;
        this.location = location;
    }

    public String getLabel() {
        if (ifLabel != null) {
            return ifLabel.toSherrlocFmt();
        } else {
            return null;
        }
    }

    static Genson genson = new GensonBuilder().useClassMetadata(true).useIndentation(true).useRuntimeType(true).create();
    @Override
    public String toString() {
        return genson.serialize(this);
    }
}

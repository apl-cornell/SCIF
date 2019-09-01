package typecheck;

import com.owlike.genson.Genson;
import com.owlike.genson.GensonBuilder;

public class VarInfo {
    public String fullName;
    public String localName;
    public TypeInfo type;

    public CodeLocation location;

    public VarInfo() {
        this.fullName = "UNKNOWN";
        this.localName = "UNKNOWN";
        this.type = new TypeInfo();
        this.location = new CodeLocation();
    }

    public VarInfo(String fullName, String localName, TypeInfo type, CodeLocation location) {
        this.fullName = fullName;
        this.localName = localName;
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
        return fullName;// + ".." + "lblVar";
    }
    public String labelToSherrlocFmt() {return  fullName + ".." + "lbl"; }
}

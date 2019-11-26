package typecheck;

import java.util.ArrayList;
import java.util.HashMap;

public class StructType extends Type {
    // Struct type should be initialized in a way like: varName = new typeName(member0, member1, ..., )
    ArrayList<VarInfo> members;

    public StructType(String typeName, ArrayList<VarInfo> members) {
        this.typeName = typeName;
        this.members = members;
    }

    public String getMemberLabel(String name) {
        for (VarInfo mb : members) {
            if (mb.localName.equals(name)) {
                return mb.typeInfo.ifl.toSherrlocFmt();
            }
        }
        return null;
    }
}

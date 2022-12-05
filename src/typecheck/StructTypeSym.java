package typecheck;

import java.util.ArrayList;

public class StructTypeSym extends TypeSym {
    // Struct type should be initialized in a way like: varName = new typeName(member0, member1, ..., )
    ArrayList<VarSym> members;

    public StructTypeSym(String typeName, ArrayList<VarSym> members) {
        super(typeName);
        this.members = members;
    }

    public String getMemberLabel(String name) {
        for (VarSym mb : members) {
            if (mb.name.equals(name)) {
                return mb.ifl.toSherrlocFmt("");
            }
        }
        return null;
    }

    public VarSym getMemberVarInfo(String prefix, String memberName) {
        for (VarSym mb : members) {
            if (mb.name.equals(memberName)) {
                VarSym rtn = new VarSym(mb);
                rtn.name = prefix + "." + rtn.name;
                return rtn;
            }
        }
        return null;
    }
}

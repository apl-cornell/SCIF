package typecheck;

import java.util.ArrayList;

public class StructTypeSym extends TypeSym {
    // Struct type should be initialized in a way like: varName = new typeName(member0, member1, ..., )
    ArrayList<VarSym> members;
    ScopeContext scopeContext;

    public StructTypeSym(String typeName, ArrayList<VarSym> members, ScopeContext scopeContext) {
        super(typeName, scopeContext);
        this.members = members;
        this.scopeContext = scopeContext;
    }

    public String getMemberLabel(String name) {
        for (VarSym mb : members) {
            if (mb.getName().equals(name)) {
                return mb.ifl.toSHErrLocFmt();
            }
        }
        return null;
    }

    public VarSym getMemberVarInfo(String prefix, String memberName) {
        for (VarSym mb : members) {
            if (mb.getName().equals(memberName)) {
                VarSym rtn = new VarSym(mb);
                rtn.setName(prefix + "." + rtn.getName());
                return rtn;
            }
        }
        return null;
    }
}

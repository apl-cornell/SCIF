package typecheck;

import ast.EventDef;
import compile.ast.Type;

import java.util.List;
import java.util.Objects;

public class EventTypeSym extends TypeSym {
    private final List<VarSym> parameters;

    public EventTypeSym(String typeName, List<VarSym> parameters, ScopeContext defContext) {
        super(typeName, defContext);
        assert parameters != null;
        this.parameters = parameters;
        this.hashCode = Objects.hash(this.getName(), parameters);
    }

    public List<VarSym> parameters() {
        return parameters;
    }

    public String labelNameSLC() {return  toSHErrLocFmt() + ".." + "lbl"; }

    public VarSym getMemberVarInfo(String prefix, String memberName) {
        for (VarSym mb : parameters) {
            if (mb.getName().equals(memberName)) {
                VarSym rtn = new VarSym(mb);
                rtn.setName(prefix + "." + rtn.getName());
                return rtn;
            }
        }
        return null;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o)
            return true;
        if (o == null || getClass() != o.getClass())
            return false;
        EventTypeSym that = (EventTypeSym) o;
        return this.hashCode == that.hashCode;
    }

    @Override
    public int hashCode() {
        return this.hashCode;
    }

    @Override
    public Type getType() {
        return null;
    }
}

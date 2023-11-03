package typecheck;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

public class ExceptionTypeSym extends TypeSym {
    // Struct type should be initialized in a way like: varName = new typeName(member0, member1, ..., )
    private final List<VarSym> parameters;

    public ExceptionTypeSym(String typeName, List<VarSym> parameters, ScopeContext defContext) {
        super(typeName, defContext);
        assert parameters != null;
        this.parameters = parameters;
        this.hashCode = Objects.hash(this.getName(), parameters);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o)
            return true;
        if (o == null || getClass() != o.getClass())
            return false;
        ExceptionTypeSym that = (ExceptionTypeSym) o;
        return this.hashCode == that.hashCode;
    }

    @Override
    public int hashCode() {
        return this.hashCode;
    }
    public String labelNameSLC() {return  toSHErrLocFmt() + ".." + "lbl"; }
    /**
     * Return the value of this symbol's label in SHErrLoc format
     * @return
     */
//    public String getLabelValueSLC() {
//        return ifl.toSHErrLocFmt();
//    }

    public List<VarSym> parameters() {
        return parameters;
    }
}

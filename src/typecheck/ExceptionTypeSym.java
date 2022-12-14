package typecheck;

import ast.IfLabel;
import java.util.ArrayList;
import java.util.Objects;

public class ExceptionTypeSym extends TypeSym {
    // Struct type should be initialized in a way like: varName = new typeName(member0, member1, ..., )
    public ArrayList<VarSym> parameters;
    public IfLabel ifl;

    public ExceptionTypeSym(String typeName, IfLabel ifl, ArrayList<VarSym> parameters) {
        super(typeName);
        // System.err.println("ExceptionTypeSym: " + typeName);
        //this.name = typeName;
        this.ifl = ifl;
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
}

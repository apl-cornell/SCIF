package typecheck;

import java.util.ArrayList;

public class ExceptionTypeSym extends TypeSym {
    // Struct type should be initialized in a way like: varName = new typeName(member0, member1, ..., )
    public ArrayList<VarSym> parameters;

    public ExceptionTypeSym(String typeName, ArrayList<VarSym> parameters) {
        // System.err.println("ExceptionTypeSym: " + typeName);
        this.name = typeName;
        this.parameters = parameters;
    }
}

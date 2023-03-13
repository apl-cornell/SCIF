package typecheck;

public class BuiltinTypeSym extends TypeSym {
    public BuiltinTypeSym(String typeName) {
        super(typeName, Utils.globalScopeContext());
        // this.name = typeName;
    }
}

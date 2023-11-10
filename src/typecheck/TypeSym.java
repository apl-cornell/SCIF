package typecheck;

public abstract class TypeSym extends Sym {

    public TypeSym(String name, ScopeContext defContext) {
        super(name, defContext);
    }

    public boolean isLValue() {
        return false;
    }

    public boolean isVoid() {
        return false;
    }
}

package typecheck;

public abstract class TypeSym extends Sym {
    public TypeSym(String name) {
        super(name);
    }

    public boolean isLValue() {
        return false;
    }
}

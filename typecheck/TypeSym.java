package typecheck;

public abstract class TypeSym extends Sym {
    public boolean isLValue() {
        return false;
    }
}

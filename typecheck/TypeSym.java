package typecheck;

public class TypeSym extends Sym {
    Type type;
    public TypeSym(String id, Type type) {
        this.name = id;
        this.type = type;
    }
    public boolean isLValue() {
        return false;
    }
}

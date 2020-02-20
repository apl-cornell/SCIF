package typecheck;

public abstract class Sym {
    public String name;
    abstract boolean isLValue();
    public String getSLCName() {
        return name;
    }
}

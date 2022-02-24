package typecheck;

public abstract class Sym {
    public String name;
    abstract boolean isLValue();
    public String getSLCName() {
        return name;
    }
    public String getName() { return name; }
    public int hashCode() {
        return name.hashCode();
    }
    public boolean equals(Object o) {
        if (!(o instanceof Sym))
            return false;
        Sym other = (Sym) o;
        return this.hashCode() == other.hashCode();
    }
}

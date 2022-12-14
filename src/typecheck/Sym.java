package typecheck;

public abstract class Sym {
    private String name;
    protected int hashCode;
    abstract boolean isLValue();
    public String getSLCName() {
        return name;
    }
    public String getName() { return name; }

    public Sym(String name) {
        this.name = name;
        hashCode = name.hashCode();
    }

    @Override
    public int hashCode() {
        return this.hashCode;
    }

    @Override
    public boolean equals(Object o) {
        if (!(o instanceof Sym))
            return false;
        Sym other = (Sym) o;
        return this.hashCode() == other.hashCode();
    }

    protected void setName(String s) {
        name = s;
    }
}

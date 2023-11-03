package typecheck;

public abstract class Sym {
    private String name;
    private ScopeContext defContext;
    private boolean isGlobal = false;
    protected int hashCode;
    abstract boolean isLValue();
    /*public String getSLCName() {
        return name;
    }*/
    public String getName() { return name; }

    public Sym(String name, ScopeContext defContext) {
        assert defContext != null;
        this.name = name;
        this.defContext = defContext;
        hashCode = name.hashCode();
    }
    public Sym(String name, ScopeContext defContext, boolean isGlobal) {
        assert defContext != null;
        this.name = name;
        this.defContext = defContext;
        this.isGlobal = isGlobal;
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

    final public String toSHErrLocFmt() {
        return defContext.getSHErrLocName() + "." + getLocalName();
    }

    private String getLocalName() {
        return name;
    }

    protected void setName(String s) {
        name = s;
    }

    public ScopeContext defContext() {
        return defContext;
    }

    public boolean isGlobal() {
        return isGlobal;
    }

}

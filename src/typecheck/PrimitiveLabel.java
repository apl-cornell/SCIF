package typecheck;

public class PrimitiveLabel extends Label {
    VarSym sym;

    public PrimitiveLabel(VarSym sym, CodeLocation location) {
        super(location);
        assert sym != null;
        this.sym = sym;
    }

    @Override
    public String toSHErrLocFmt() {
        return sym.toSHErrLocFmt();
    }

    @Override
    public String toSHErrLocFmt(String origin, String substitution) {
        if (sym.toSHErrLocFmt().equals(origin)) {
            return substitution;
        }
        return sym.toSHErrLocFmt();
    }
}

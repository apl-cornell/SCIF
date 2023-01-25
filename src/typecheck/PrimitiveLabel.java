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
}

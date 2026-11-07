package typecheck;

import java.util.List;
import java.util.Map;

public class PrimitiveLabel extends Label {
    VarSym sym;
    /** The version of {@code sym} this label names, or null if it names {@code sym} itself. */
    private final String version;

    @Override
    public List<String> principalLeaves() {
        return List.of(sym.getName());
    }

    @Override
    public List<VarSym> principalSyms() {
        return List.of(sym);
    }

    /** A label naming the principal held by {@code sym}. */
    public PrimitiveLabel(VarSym sym, CodeLocation location) {
        this(sym, null, location);
    }

    /**
     * A label naming {@code version}, a version of the principal variable {@code sym}.
     * If {@code version} is null, the label names {@code sym} itself.
     */
    public PrimitiveLabel(VarSym sym, String version, CodeLocation location) {
        super(location);
        assert sym != null;
        this.sym = sym;
        this.version = version;
    }

    @Override
    public String toSHErrLocFmt() {
        return version != null ? version : sym.toSHErrLocFmt();
    }

    @Override
    public String toSHErrLocFmt(String origin, String substitution) {
        String str = toSHErrLocFmt();
        return str.equals(origin) ? substitution : str;
    }

    @Override
    public String toSHErrLocFmt(Map<String, String> mapping) {
        String str = toSHErrLocFmt();
        return mapping.getOrDefault(str, str);
    }
}

package typecheck;

import java.util.List;
import java.util.Map;

public class PrimitiveLabel extends Label {
    VarSym sym;

    @Override
    public List<String> principalLeaves() {
        return List.of(sym.getName());
    }

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
    @Override
    public String toSHErrLocFmt(Map<String, String> mapping) {
        String str = sym.toSHErrLocFmt();
        return mapping.getOrDefault(str, str);
    }
}

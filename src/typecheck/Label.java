package typecheck;

import java.util.Map;

/**
 * Represent an information flow label.
 * Could be a primitive label: a var symbol
 * Or a combination of two labels.
 */
abstract public class Label {
    final protected CodeLocation location;

    protected Label(CodeLocation location) {
        this.location = location;
    }

    abstract public String toSHErrLocFmt();
    abstract public String toSHErrLocFmt(String origin, String substitution);
    abstract public String toSHErrLocFmt(Map<String, String> mapping);

    public CodeLocation location() {
        return location;
    }
}

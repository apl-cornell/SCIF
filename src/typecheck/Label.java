package typecheck;

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

    public CodeLocation location() {
        return location;
    }
}

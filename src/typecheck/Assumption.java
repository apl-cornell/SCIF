package typecheck;

import typecheck.sherrlocUtils.Inequality;
import typecheck.sherrlocUtils.Relation;

/**
 * Represent an assumption in a contract. Such as
 * a trust relationship.
 */
public class Assumption {
    private final PrimitiveLabel left, right;
    private final Relation optor;
    private final CodeLocation location;


    public Assumption(PrimitiveLabel left, Relation optor, PrimitiveLabel right, CodeLocation location) {
        this.left = left;
        this.right = right;
        this.optor = optor;
        this.location = location;
    }

    public Inequality toInequality() {
        return new Inequality(left.toSHErrLocFmt(),
                optor,
                right.toSHErrLocFmt());
    }

    public CodeLocation location() {
        return location;
    }
}

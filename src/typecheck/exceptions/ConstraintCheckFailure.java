package typecheck.exceptions;

import typecheck.CodeLocation;

public class ConstraintCheckFailure extends TypeCheckFailure {
    String log;
    public ConstraintCheckFailure(String log, CodeLocation loc) {
        super(log, loc);
        this.log = log;
    }
}

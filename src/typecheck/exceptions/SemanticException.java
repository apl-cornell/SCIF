package typecheck.exceptions;

import typecheck.CodeLocation;

public class SemanticException extends Exception {
    CodeLocation location;
    public SemanticException(String message, CodeLocation loc) {
        super(message);
        location = loc;
    }
    @Override public String getMessage() {
        return location.errString() + ": " + super.getMessage();
    }
}

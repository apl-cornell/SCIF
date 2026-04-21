package typecheck.exceptions;

import typecheck.CodeLocation;
import typecheck.ErrorFormatter;

public class SemanticException extends Exception {
    CodeLocation location;
    public SemanticException(String message, CodeLocation loc) {
        super(message);
        location = loc;
    }
    @Override public String getMessage() {
        return ErrorFormatter.format(location, super.getMessage());
    }
}

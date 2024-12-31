package typecheck.exceptions;

import typecheck.CodeLocation;

public class NameNotFoundException extends SemanticException {
    public NameNotFoundException(String name, CodeLocation location) {
        super("Unrecognized name: " + name, location);
    }
}

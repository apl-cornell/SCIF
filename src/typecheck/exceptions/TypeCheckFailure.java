package typecheck.exceptions;

import typecheck.CodeLocation;

public class TypeCheckFailure extends SemanticException {
    public TypeCheckFailure(String message, CodeLocation loc) {
        super(message, loc);
    }
}
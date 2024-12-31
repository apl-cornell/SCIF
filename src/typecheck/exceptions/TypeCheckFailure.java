package typecheck.exceptions;

import typecheck.CodeLocation;

abstract public class TypeCheckFailure extends SemanticException {
    public TypeCheckFailure(String message, CodeLocation loc) {
        super(message, loc);
    }
    abstract public String explanation();
}
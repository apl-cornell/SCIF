package ast;

import ast.Type;
import typecheck.ScopeContext;

import java.util.HashMap;

/** A type variable to be solved for. */
public class TypeVar extends Type {
    static HashMap<String, TypeVar> typeVars = new HashMap<>();
    Type solvedType; // null if unsolved
    public TypeVar(String basename) {
        super(freshen(basename));
        typeVars.put(name, this);
        solvedType = null;
    }

    private static String freshen(String name) {
        int count = 0;
        String fresh = name;
        while (typeVars.containsKey(fresh)) {
            count++;
            fresh = name + count;
        }
        return fresh;
    }
}

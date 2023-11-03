package ast;

import compile.CompileEnv;
import compile.ast.Statement;
import compile.ast.Type;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import typecheck.*;

public abstract class Expression extends Node {

    public VarSym getVarInfo(VisitEnv env, boolean tail_position) {
        return null;
    }

    public VarSym getVarInfo(NTCEnv env) {
        return null;
    }
//
//    public String toSolCode() {
//        assert false;
//        return "unknown";
//    }

    public abstract ExpOutcome genConsVisit(VisitEnv env, boolean tail_position);

    /*
        Check whether there are non-exception-free calls inside the expression.
        If so, create temporary variables and statements to eventually evaluate the expression.
        Otherwise, normally create expressions.
        Return the final result as an expression.

     */
    public abstract compile.ast.Expression solidityCodeGen(List<Statement> result, CompileEnv code);

    public abstract boolean typeMatch(Expression expression);

    public Map<String, Type> readMap(CompileEnv code) {
        return new HashMap<>();
    }

    public Map<String,? extends Type> writeMap(CompileEnv code) {
        return new HashMap<>();
    }

    // public abstract boolean isCallFree();

}

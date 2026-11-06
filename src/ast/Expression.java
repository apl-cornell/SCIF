package ast;

import compile.CompileEnv;
import compile.ast.Statement;
import compile.ast.Type;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import typecheck.*;
import typecheck.exceptions.SemanticException;

public abstract class Expression extends Node {

    public VarSym getVarInfo(VisitEnv env, boolean tail_position, Map<String, String> dependentMapping) throws SemanticException {
        throw new AssertionError();
    }

    /**
     * Returns the variable this expression denotes, or null if it does not
     * denote a variable.
     *
     * @throws SemanticException if it names a variable that cannot be resolved
     */
    public VarSym getVarInfo(NTCEnv env) throws SemanticException {
        return null;
    }
//
//    public String toSolCode() {
//        assert false;
//        return "unknown";
//    }

    /** The static type of this expression, once it has been checked for information flow. */
    private TypeSym type;

    /**
     * Generates the information-flow constraints for this expression, evaluated in the context
     * {@code env.inContext}, and returns the label of its value and the paths by which its
     * evaluation can end. Afterwards {@link #type()} is defined.
     */
    public final ExpOutcome genIFConstraints(VisitEnv env, boolean tail_position)
            throws SemanticException {
        ExpOutcome outcome = genFlowConstraints(env, tail_position);
        type = staticType(env);
        if (type == null) {
            throw new AssertionError("no static type for " + getClass().getSimpleName()
                    + " at " + location);
        }
        return outcome;
    }

    /**
     * Generates the information-flow constraints for this expression, as specified for
     * {@link #genIFConstraints}.
     */
    protected abstract ExpOutcome genFlowConstraints(VisitEnv env, boolean tail_position)
            throws SemanticException;

    /**
     * The static type of this expression in {@code env}, called once its information-flow
     * constraints have been generated in {@code env}.
     */
    protected abstract TypeSym staticType(VisitEnv env) throws SemanticException;

    /**
     * The static type of this expression.
     *
     * @throws AssertionError if this expression has not been checked for information flow
     */
    public TypeSym type() {
        if (type == null) {
            throw new AssertionError("no static type for " + getClass().getSimpleName()
                    + " at " + location);
        }
        return type;
    }

    /**
     * If this expression is a principal-typed variable, or a cast of one to a contract or
     * address type, the SHErrLoc name of the principal it holds at the current program point.
     * Otherwise null.
     */
    public String principalName(VisitEnv env) {
        return null;
    }

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

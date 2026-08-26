package ast;

import compile.CompileEnv;
import compile.ast.Statement;
import java.util.ArrayList;
import java.util.List;
import typecheck.ExpOutcome;
import typecheck.NTCEnv;
import typecheck.ScopeContext;
import typecheck.VisitEnv;
import typecheck.exceptions.SemanticException;

/**
 * `_` placeholder for an unbound parameter position in a
 * {@link ClosureCreation}. Carries no value; only its position matters.
 * Never appears outside a closure-creation argument list.
 */
public class Hole extends Expression {

    @Override
    public ScopeContext genTypeConstraints(NTCEnv env, ScopeContext parent)
            throws SemanticException {
        throw new UnsupportedOperationException(
                "hole `_` has no type outside closure creation");
    }

    @Override
    public ExpOutcome genIFConstraints(VisitEnv env, boolean tail_position)
            throws SemanticException {
        throw new UnsupportedOperationException(
                "hole `_` has no IFC constraints outside closure creation");
    }

    @Override
    public compile.ast.Expression solidityCodeGen(List<Statement> result, CompileEnv code) {
        throw new UnsupportedOperationException(
                "hole `_` is not directly compiled");
    }

    @Override
    public boolean typeMatch(Expression expression) {
        return expression instanceof Hole;
    }

    @Override
    public List<Node> children() {
        return new ArrayList<>();
    }
}

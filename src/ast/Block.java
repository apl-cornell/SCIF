package ast;

import compile.CompileEnv;
import compile.ast.Type;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import typecheck.NTCEnv;
import typecheck.PathOutcome;
import typecheck.PsiUnit;
import typecheck.ScopeContext;
import typecheck.Utils;
import typecheck.VisitEnv;
import typecheck.exceptions.SemanticException;

/**
 * A sequence of statements executed in order, such as the body of a method or a branch of a
 * conditional. A block does not introduce a scope; the construct that contains it decides
 * which scopes its statements are in.
 */
public class Block extends Statement {

    private final List<Statement> statements;

    public Block(List<Statement> statements) {
        this.statements = new ArrayList<>(statements);
    }

    /** The statements of this block, in order. */
    public List<Statement> statements() {
        return Collections.unmodifiableList(statements);
    }

    /** Whether this block has no statements. */
    public boolean isEmpty() {
        return statements.isEmpty();
    }

    /**
     * Generates the type constraints of each statement in the scope context {@code parent},
     * and returns the scope context of the last statement, or {@code parent} if there is none.
     */
    @Override
    public ScopeContext genTypeConstraints(NTCEnv env, ScopeContext parent)
            throws SemanticException {
        ScopeContext last = parent;
        for (Statement s : statements) {
            last = s.genTypeConstraints(env, parent);
        }
        return last;
    }

    /**
     * Generates the information-flow constraints for this block, which starts in the context
     * {@code env.inContext}, and returns every way it can terminate: its exception and return
     * paths, and its normal path if it can complete normally. The last statement is in tail
     * position if {@code tail_position} is. On return, {@code env.inContext} is the context
     * after the last statement that completed normally.
     */
    @Override
    public PathOutcome IFCVisit(VisitEnv env, boolean tail_position) throws SemanticException {
        PathOutcome result = new PathOutcome();
        int index = 0;
        for (Statement s : statements) {
            ++index;
            String prevLambda = env.inContext.lambda;
            boolean isTail = index == statements.size() && tail_position;
            PathOutcome so = s.IFCVisit(env, isTail);
            PsiUnit normal = so.getNormalPath();
            result.join(so.withoutNormalPath());
            if (normal == null) {
                return result;
            }
            env.inContext = Utils.genNewContextAndConstraints(env, isTail, normal.c, prevLambda,
                    s.nextPcSHL(), s.location());
        }
        result.setNormalPath(env.inContext);
        return result;
    }

    @Override
    public List<compile.ast.Statement> solidityCodeGen(CompileEnv code) {
        List<compile.ast.Statement> result = new ArrayList<>();
        for (Statement s : statements) {
            result.addAll(s.solidityCodeGen(code));
        }
        return result;
    }

    @Override
    public List<Node> children() {
        return new ArrayList<>(statements);
    }

    @Override
    public boolean exceptionHandlingFree() {
        for (Statement s : statements) {
            if (!s.exceptionHandlingFree()) {
                return false;
            }
        }
        return true;
    }

    @Override
    protected Map<String, ? extends Type> readMap(CompileEnv code) {
        Map<String, Type> result = new HashMap<>();
        for (Statement s : statements) {
            result.putAll(s.readMap(code));
        }
        return result;
    }

    @Override
    protected Map<String, ? extends Type> writeMap(CompileEnv code) {
        Map<String, Type> result = new HashMap<>();
        for (Statement s : statements) {
            result.putAll(s.writeMap(code));
        }
        return result;
    }
}

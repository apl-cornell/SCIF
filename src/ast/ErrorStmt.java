package ast;

import compile.CompileEnv;
import java.util.List;
import typecheck.NTCEnv;
import typecheck.PathOutcome;
import typecheck.ScopeContext;
import typecheck.VisitEnv;

public class ErrorStmt extends Statement {

    @Override
    public ScopeContext generateConstraints(NTCEnv env, ScopeContext parent) {
        return null;
    }

    @Override
    public List<compile.ast.Statement> solidityCodeGen(CompileEnv code) {
        return null;
    }

    @Override
    public List<Node> children() {
        return null;
    }

    @Override
    public PathOutcome genConsVisit(VisitEnv env, boolean tail_position) {
        return null;
    }
}

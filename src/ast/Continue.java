package ast;

import compile.CompileEnv;
import java.util.ArrayList;
import java.util.List;
import typecheck.NTCEnv;
import typecheck.PathOutcome;
import typecheck.ScopeContext;
import typecheck.VisitEnv;

public class Continue extends Statement {

    @Override
    public PathOutcome genConsVisit(VisitEnv env, boolean tail_position) {
        return null;
    }

    @Override
    public ScopeContext ntcGenCons(NTCEnv env, ScopeContext parent) {
        return null;
    }

    @Override
    public List<compile.ast.Statement> solidityCodeGen(CompileEnv code) {
        return List.of(new compile.ast.Continue());
    }

    @Override
    public List<Node> children() {
        return new ArrayList<>();
    }
}

package ast;

import compile.CompileEnv;
import java.util.ArrayList;
import java.util.List;
import typecheck.NTCEnv;
import typecheck.PathOutcome;
import typecheck.ScopeContext;
import typecheck.VisitEnv;

public class Break extends Statement {

    @Override
    public ScopeContext genTypeConstraints(NTCEnv env, ScopeContext parent) {
        return null;
    }

    @Override
    public List<compile.ast.Statement> solidityCodeGen(CompileEnv code) {
        return List.of(new compile.ast.Break());
    }

    @Override
    public PathOutcome IFCVisit(VisitEnv env, boolean tail_position) {
        return null;
    }
    @Override
    public List<Node> children() {
        List<Node> rtn = new ArrayList<>();
        return rtn;
    }
}

package ast;

import compile.CompileEnv;
import java.util.ArrayList;
import java.util.List;
import typecheck.NTCEnv;
import typecheck.PathOutcome;
import typecheck.ScopeContext;
import typecheck.VisitEnv;

public class AugAssign extends Statement {

    Expression target;
    BinaryOperator op;
    Expression value;

    public AugAssign(Expression target, BinaryOperator op, Expression value) {
        this.target = target;
        this.op = op;
        this.value = value;
    }

    @Override
    public PathOutcome genConsVisit(VisitEnv env, boolean tail_position) {
        return null;
    }

    @Override
    public ScopeContext genTypeConstraints(NTCEnv env, ScopeContext parent) {
        return null;
    }

    @Override
    public List<compile.ast.Statement> solidityCodeGen(CompileEnv code) {
        assert false;
        return null;
    }
    @Override
    public List<Node> children() {
        List<Node> rtn = new ArrayList<>();
        rtn.add(target);
        rtn.add(value);
        return rtn;
    }
}

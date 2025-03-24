package ast;

import compile.CompileEnv;
import compile.ast.Type;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import typecheck.NTCEnv;
import typecheck.PathOutcome;
import typecheck.ScopeContext;
import typecheck.VisitEnv;
import typecheck.exceptions.SemanticException;

public class CallStatement extends Statement {

    Call call;

    public CallStatement(Call call) {
        this.call = call;
    }

    public CallStatement(CallStatement callStatement) {
        this.call = new Call(callStatement.call);
    }

    @Override
    public ScopeContext generateConstraints(NTCEnv env, ScopeContext parent) throws SemanticException {
        return call.generateConstraints(env, parent);
    }

    @Override
    public List<compile.ast.Statement> solidityCodeGen(CompileEnv code) {
        List<compile.ast.Statement> result = new ArrayList<>();
        result.add(call.solidityCodeGen(result, code));
        return result;
    }

    @Override
    public PathOutcome genConsVisit(VisitEnv env, boolean tail_position) {
        return call.genConsVisit(env, tail_position).psi;
    }

    @Override
    public ArrayList<Node> children() {
        ArrayList<Node> rtn = new ArrayList<>();
        rtn.add(call);
        return rtn;
    }

    @Override
    protected Map<String,? extends Type> readMap(CompileEnv code) {
        return call.readMap(code);
    }
}

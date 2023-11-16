package ast;

import compile.CompileEnv;
import compile.ast.Statement;
import java.util.List;
import typecheck.ExpOutcome;
import typecheck.NTCEnv;
import typecheck.ScopeContext;
import typecheck.VisitEnv;

public class New extends Expression {
    Call constructor_call;

    public New(Call constructor_call) {
        this.constructor_call = constructor_call;
    }

    @Override
    public ScopeContext ntcGenCons(NTCEnv env, ScopeContext parent) {
        // check constructor_call to be a valid contract constructor call
        return null;
    }

    @Override
    public ExpOutcome genConsVisit(VisitEnv env, boolean tail_position) {
        // creating contract requires pc and arguments to be of integirty level `this`
        return null;
    }

    @Override
    public compile.ast.Expression solidityCodeGen(List<Statement> result, CompileEnv code) {
        return new compile.ast.New(contractName, argExps);
    }

    @Override
    public boolean typeMatch(Expression expression) {
        return expression instanceof New && constructor_call.typeMatch(((New) expression).constructor_call);
    }

    @Override
    public List<Node> children() {
        return List.of(constructor_call);
    }
}

package ast;

import compile.CompileEnv;
import compile.ast.Statement;
import compile.ast.UnaryOperation;
import java.util.List;

import typecheck.exceptions.SemanticException;
import typecheck.sherrlocUtils.Relation;
import typecheck.*;

import java.util.ArrayList;

public class UnaryOp extends Expression {

    UnaryOperator op;
    Expression operand;

    public UnaryOp(UnaryOperator x, Expression y) {
        op = x;
        operand = y;
    }

    public ScopeContext genTypeConstraints(NTCEnv env, ScopeContext parent) throws SemanticException {
        ScopeContext now = new ScopeContext(this, parent);
        ScopeContext rtn = operand.genTypeConstraints(env, now);
        env.addCons(now.genTypeConstraints(rtn, Relation.EQ, env, location));
        if (op == UnaryOperator.USub || op == UnaryOperator.UAdd) {
            env.addCons(
                    rtn.genTypeConstraints(Utils.BuiltinType2ID(BuiltInT.UINT), Relation.EQ, env, location));
        } else if (op == UnaryOperator.Not || op == UnaryOperator.Invert) {
            env.addCons(
                    rtn.genTypeConstraints(Utils.BuiltinType2ID(BuiltInT.BOOL), Relation.EQ, env, location));
        }
        return now;
    }

    @Override
    public ExpOutcome genIFConstraints(VisitEnv env, boolean tail_position) {
        return operand.genIFConstraints(env, tail_position);
    }

    @Override
    public compile.ast.Expression solidityCodeGen(List<Statement> result, CompileEnv code) {
        return new UnaryOperation(compile.Utils.toUnaryOp(op), operand.solidityCodeGen(result, code));
    }

    @Override
    public boolean typeMatch(Expression expression) {
        return expression instanceof UnaryOp &&
                op == ((UnaryOp) expression).op &&
                operand.typeMatch(((UnaryOp) expression).operand);
    }

    @Override
    public ArrayList<Node> children() {
        ArrayList<Node> rtn = new ArrayList<>();
        rtn.add(operand);
        return rtn;
    }
    @Override
    public java.util.Map<String, compile.ast.Type> readMap(CompileEnv code) {
        return operand.readMap(code);
    }
}

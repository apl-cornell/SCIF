package ast;

import sherrlocUtils.Relation;
import typecheck.*;

import java.util.ArrayList;

public class UnaryOp extends Expression {
    UnaryOperator op;
    Expression operand;
    public UnaryOp(UnaryOperator x, Expression y) {
        op = x;
        operand = y;
    }

    public ScopeContext NTCgenCons(NTCEnv env, ScopeContext parent) {
        ScopeContext now = new ScopeContext(this, parent);
        ScopeContext rtn = operand.NTCgenCons(env, now);
        env.addCons(now.genCons(rtn, Relation.EQ, env, location));
        if (op == UnaryOperator.USub || op == UnaryOperator.UAdd) {
            env.addCons(rtn.genCons(Utils.BuiltinType2ID(BuiltInT.UINT), Relation.EQ, env, location));
        } else if (op == UnaryOperator.Not || op == UnaryOperator.Invert) {
            env.addCons(rtn.genCons(Utils.BuiltinType2ID(BuiltInT.BOOL), Relation.EQ, env, location));
        }
        return now;
    }

    @Override
    public Context genConsVisit(VisitEnv env, boolean tail_position) {
        return operand.genConsVisit(env, tail_position);
    }

    @Override
    public String toSolCode() {
        String v = operand.toSolCode();
        return compile.Utils.toUnaryOp(op) + v;
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
}

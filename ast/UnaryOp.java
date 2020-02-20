package ast;

import sherrlocUtils.Relation;
import typecheck.*;

import java.util.ArrayList;
import java.util.HashMap;

public class UnaryOp extends Expression {
    UnaryOperator op;
    Expression operand;
    public UnaryOp(UnaryOperator x, Expression y) {
        op = x;
        operand = y;
    }

    public NTCContext NTCgenCons(NTCEnv env, NTCContext parent) {
        NTCContext now = new NTCContext(this, parent);
        NTCContext rtn = operand.NTCgenCons(env, now);
        env.addCons(now.genCons(rtn, Relation.EQ, env, location));
        if (op == UnaryOperator.USub || op == UnaryOperator.UAdd) {
            env.addCons(rtn.genCons(Utils.BuiltinType2ID(BuiltInT.INT), Relation.EQ, env, location));
        } else if (op == UnaryOperator.Not || op == UnaryOperator.Invert) {
            env.addCons(rtn.genCons(Utils.BuiltinType2ID(BuiltInT.BOOL), Relation.EQ, env, location));
        }
        return now;
    }

    @Override
    public Context genConsVisit(VisitEnv env) {
        return operand.genConsVisit(env);
    }
}

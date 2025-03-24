package ast;

import compile.CompileEnv;
import compile.ast.Literal;
import compile.ast.Statement;
import java.util.ArrayList;
import java.util.List;
import typecheck.sherrlocUtils.Constraint;
import typecheck.sherrlocUtils.Inequality;
import typecheck.sherrlocUtils.Relation;
import typecheck.*;

public class ConstantExpr extends Expression {

    Constant value;

    public ConstantExpr(Constant value) {
        this.value = value;
    }

    @Override
    public ScopeContext generateConstraints(NTCEnv env, ScopeContext parent) {
        //TODO: Constant are boolean
        ScopeContext now = new ScopeContext(this, parent);
        env.addCons(now.genCons(env.getSymName(BuiltInT.BOOL), Relation.EQ, env, location));
        return now;
    }

    @Override
    public ExpOutcome genConsVisit(VisitEnv env, boolean tail_position) {

        Context beginContext = env.inContext;
        Context endContext = new Context(typecheck.Utils.getLabelNamePc(toSHErrLocFmt()),
                typecheck.Utils.getLabelNameLock(toSHErrLocFmt()));
        //env.outContext = endContext;
        env.cons.add(new Constraint(new Inequality(beginContext.pc, endContext.pc), env.hypothesis(),
                location, env.curContractSym().getName(),
                "Integrity of the control flow doesn't flow to value of this constant"));
        if (!tail_position) {
            env.cons.add(new Constraint(new Inequality(endContext.lambda, beginContext.lambda),
                    env.hypothesis(), location, env.curContractSym().getName(),
                    typecheck.Utils.ERROR_MESSAGE_LOCK_IN_NONLAST_OPERATION));
        }
        return new ExpOutcome(endContext.pc, new PathOutcome(new PsiUnit(endContext)));
    }

    @Override
    public compile.ast.Expression solidityCodeGen(List<Statement> result, CompileEnv code) {
        return new Literal(compile.Utils.toConstant(value));
    }

//    @Override
//    public String toSolCode() {
//        return compile.Utils.toConstant(value);
//    }

    @Override
    public boolean typeMatch(Expression expression) {
        return expression instanceof ConstantExpr &&
                value.equals(((ConstantExpr) expression).value);
    }
    @Override
    public List<Node> children() {
        List<Node> rtn = new ArrayList<>();
        return rtn;
    }
}

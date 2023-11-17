package ast;

import compile.CompileEnv;
import compile.Utils;
import compile.ast.BinaryExpression;
import compile.ast.Statement;
import compile.ast.Type;
import java.util.List;
import java.util.Map;
import typecheck.sherrlocUtils.Constraint;
import typecheck.sherrlocUtils.Inequality;
import typecheck.sherrlocUtils.Relation;
import typecheck.*;

import java.util.ArrayList;

public class Compare extends Expression {

    Expression left;
    CompareOperator op;
    Expression right;

    public Compare(Expression l, CompareOperator x, Expression r) {
        left = l;
        op = x;
        right = r;
    }

    @Override
    public ScopeContext ntcGenCons(NTCEnv env, ScopeContext parent) {
        //TODO: not included: Is, NotIs, In, NotIn
        ScopeContext now = new ScopeContext(this, parent);
        ScopeContext l = left.ntcGenCons(env, now), r = right.ntcGenCons(env, now);

        env.addCons(l.genCons(r, Relation.EQ, env, location));
        if (op == CompareOperator.Eq || op == CompareOperator.NotEq) {
            //env.addCons(l.genCons(r, Relation.EQ, env, location));
        } else if (op == CompareOperator.Lt || op == CompareOperator.LtE
                || op == CompareOperator.Gt || op == CompareOperator.GtE) {
            env.addCons(l.genCons(env.getSymName(BuiltInT.UINT), Relation.EQ, env, location));
        }
        env.addCons(now.genCons(env.getSymName(BuiltInT.BOOL), Relation.EQ, env, location));
        return now;
    }

    @Override
    public ExpOutcome genConsVisit(VisitEnv env, boolean tail_position) {
        Context beginContext = env.inContext;
        Context endContext = new Context(typecheck.Utils.getLabelNamePc(toSHErrLocFmt()),
                typecheck.Utils.getLabelNameLock(toSHErrLocFmt()));

        env.inContext = beginContext;
        ExpOutcome lo = left.genConsVisit(env, false);
        String ifNameLeft = lo.valueLabelName;

        env.inContext = typecheck.Utils.genNewContextAndConstraints(env, false, lo.psi.getNormalPath().c, beginContext.lambda, left.nextPcSHL(), left.location);
//        env.inContext = new Context(lo.psi.getNormalPath().c.pc, beginContext.lambda);
        ExpOutcome ro = right.genConsVisit(env, false);
        String ifNameRight = ro.valueLabelName;

        String ifNameRtn = scopeContext.getSHErrLocName() + "." + "cmp" + location.toString();

        env.cons.add(new Constraint(new Inequality(ifNameLeft, ifNameRtn), env.hypothesis(), location,
                env.curContractSym().getName(),
                "Integrity of left hand expression doesn't flow to value of this compare operation"));
        env.cons.add(
                new Constraint(new Inequality(ifNameRight, ifNameRtn), env.hypothesis(), location,
                        env.curContractSym().getName(),
                        "Integrity of right hand expression doesn't flow to value of this compare operation"));

        typecheck.Utils.contextFlow(env, ro.psi.getNormalPath().c, endContext, right.location);
        // env.outContext = endContext;

        if (!tail_position) {
            env.cons.add(new Constraint(
                    new Inequality(ro.psi.getNormalPath().c.lambda, beginContext.lambda),
                    env.hypothesis(), location, env.curContractSym().getName(),
                    typecheck.Utils.ERROR_MESSAGE_LOCK_IN_NONLAST_OPERATION));
        }

        ro.psi.join(lo.psi);
        ro.psi.setNormalPath(endContext);

        return new ExpOutcome(ifNameRtn, ro.psi);
    }

    @Override
    public compile.ast.Expression solidityCodeGen(List<Statement> result, CompileEnv code) {
        return new BinaryExpression(Utils.toCompareOp(op), left.solidityCodeGen(result, code), right.solidityCodeGen(result, code));
    }
//
//    public String toSolCode() {
//        return CompileEnv.toCompareOp(left.toSolCode(), Utils.toCompareOp(op), right.toSolCode());
//
//    }

    @Override
    public boolean typeMatch(Expression expression) {
        return expression instanceof Compare &&
                left.typeMatch(((Compare) expression).left) &&
                op == ((Compare) expression).op &&
                right.typeMatch(((Compare) expression).right);
    }

    @Override
    public ArrayList<Node> children() {
        ArrayList<Node> rtn = new ArrayList<>();
        rtn.add(left);
        rtn.add(right);
        return rtn;
    }
    @Override
    public java.util.Map<String, compile.ast.Type> readMap(CompileEnv code) {
        Map<String, Type> result = left.readMap(code);
        result.putAll(right.readMap(code));
        return result;
    }
}

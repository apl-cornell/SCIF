package ast;

import compile.CompileEnv;
import compile.Utils;
import compile.ast.BinaryExpression;
import compile.ast.Statement;
import compile.ast.Type;
import java.util.List;
import java.util.Map;

import typecheck.exceptions.SemanticException;
import typecheck.sherrlocUtils.Constraint;
import typecheck.sherrlocUtils.Inequality;
import typecheck.sherrlocUtils.Relation;
import typecheck.*;

import java.util.ArrayList;

public class BoolOp extends Expression {

    BoolOperator op;
    Expression left, right;

    public BoolOp(Expression l, BoolOperator x, Expression r) {
        left = l;
        op = x;
        right = r;
    }

    @Override
    public ScopeContext genTypeConstraints(NTCEnv env, ScopeContext parent) throws SemanticException {
        ScopeContext now = new ScopeContext(this, parent);
        ScopeContext l = left.genTypeConstraints(env, now), r = right.genTypeConstraints(env, now);
        env.addCons(now.genTypeConstraints(l, Relation.LEQ, env, location));
        env.addCons(now.genTypeConstraints(r, Relation.LEQ, env, location));
        env.addCons(now.genTypeConstraints(env.getSymName(BuiltInT.BOOL), Relation.EQ, env, location));
        return now;
    }

    @Override
    public ExpOutcome genIFConstraints(VisitEnv env, boolean tail_position) {
        Context beginContext = env.inContext;
        Context endContext = new Context(typecheck.Utils.getLabelNamePc(toSHErrLocFmt()),
                typecheck.Utils.getLabelNameLock(toSHErrLocFmt()));

        env.inContext = beginContext;
        ExpOutcome lo = left.genIFConstraints(env, false);
        String ifNameLeft = lo.valueLabelName;

        // env.inContext = new Context(lo.psi.getNormalPath().c.pc, beginContext.lambda);
        env.inContext = typecheck.Utils.genNewContextAndConstraints(env, false, lo.psi.getNormalPath().c, beginContext.lambda, left.nextPcSHL(), left.location);
        ExpOutcome ro = right.genIFConstraints(env, false);
        String ifNameRight = ro.valueLabelName;

        String ifNameRtn = scopeContext.getSHErrLocName() + "." + "bool" + location.toString();

        env.cons.add(new Constraint(new Inequality(ifNameLeft, ifNameRtn), env.hypothesis(), location,
                env.curContractSym().getName(),
                "Integrity of left hand expression doesn't flow to value of this boolean operation"));
        env.cons.add(
                new Constraint(new Inequality(ifNameRight, ifNameRtn), env.hypothesis(), location,
                        env.curContractSym().getName(),
                        "Integrity of right hand expression doesn't flow to value of this boolean operation"));

        typecheck.Utils.contextFlow(env, ro.psi.getNormalPath().c, endContext, right.location);
        // env.outContext = endContext;

        if (!tail_position) {
            env.cons.add(new Constraint(
                    new Inequality(endContext.lambda, beginContext.lambda),
                    env.hypothesis(), location, env.curContractSym().getName(),
                    typecheck.Utils.ERROR_MESSAGE_LOCK_IN_NONLAST_OPERATION));
        }

        ro.psi.join(lo.psi);
        ro.psi.setNormalPath(endContext);

        return new ExpOutcome(ifNameRtn, ro.psi);
    }

    @Override
    public compile.ast.Expression solidityCodeGen(List<Statement> result, CompileEnv code) {
        compile.ast.Expression lExp = left.solidityCodeGen(result, code);
        compile.ast.Expression rExp = right.solidityCodeGen(result, code);
        return new BinaryExpression(Utils.toBoolOp(op), lExp, rExp);
    }

//    public String toSolCode() {
//        return CompileEnv.toBoolOp(left.toSolCode(), Utils.toBoolOp(op), right.toSolCode());
//    }

    @Override
    public boolean typeMatch(Expression expression) {
        return expression instanceof BoolOp &&
                op == ((BoolOp) expression).op &&
                left.typeMatch(((BoolOp) expression).left) &&
                right.typeMatch(((BoolOp) expression).right);
    }

    @Override
    public List<Node> children() {
        List<Node> rtn = new ArrayList<>();
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

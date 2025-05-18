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

// Assume operators are INT
public class BinOp extends Expression {

    Expression left, right;
    BinaryOperator op;

    public BinOp(Expression l, BinaryOperator x, Expression r) {
        left = l;
        op = x;
        right = r;
    }

    @Override
    public ScopeContext genTypeConstraints(NTCEnv env, ScopeContext parent) throws SemanticException {
        logger.debug("binOp:");
        ScopeContext now = new ScopeContext(this, parent);
        ScopeContext l = left.genTypeConstraints(env, now);
        // logger.debug("binOp/left:");
        // logger.debug(l.toString());
        ScopeContext r = right.genTypeConstraints(env, now);
        // logger.debug("binOp/right:");
        // logger.debug(r.toString());
        env.addCons(now.genTypeConstraints(l, Relation.LEQ, env, location));
        env.addCons(now.genTypeConstraints(r, Relation.LEQ, env, location));
        env.addCons(now.genTypeConstraints(env.getSymName(BuiltInT.UINT), Relation.EQ, env, location));
        return now;
    }

    @Override
    public ExpOutcome genIFConstraints(VisitEnv env, boolean tail_position) throws SemanticException {
        Context beginContext = env.inContext;
        Context endContext = new Context(typecheck.Utils.getLabelNamePc(toSHErrLocFmt()),
                typecheck.Utils.getLabelNameLock(toSHErrLocFmt()));

        env.inContext = beginContext;
        ExpOutcome lo = left.genIFConstraints(env, false);
        String ifNameLeft = lo.valueLabelName;

        env.inContext = typecheck.Utils.genNewContextAndConstraints(env, false, lo.psi.getNormalPath().c, beginContext.lambda, left.nextPcSHL(), left.location);
//                new Context(lo.psi.getNormalPath().c.pc, beginContext.lambda);
        ExpOutcome ro = right.genIFConstraints(env, false);
        String ifNameRight = ro.valueLabelName;

        String ifNameRtn = scopeContext.getSHErrLocName() + "." + "bin" + location.toString();

        env.cons.add(new Constraint(new Inequality(ifNameLeft, ifNameRtn), env.hypothesis(), left.location,
                env.curContractSym().getName(),
                "Integrity of left hand expression doesn't flow to value of this binary operation"));
        env.cons.add(
                new Constraint(new Inequality(ifNameRight, ifNameRtn), env.hypothesis(), right.location,
                        env.curContractSym().getName(),
                        "Integrity of right hand expression doesn't flow to value of this binary operation"));

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
        compile.ast.Expression lExp = left.solidityCodeGen(result, code);
        compile.ast.Expression rExp = right.solidityCodeGen(result, code);
        return new BinaryExpression(Utils.toBinOp(op), lExp, rExp);
    }
//
//    public String toSolCode() {
//        return CompileEnv.toBinOp(left.toSolCode(), Utils.toBinOp(op), right.toSolCode());
//    }

    @Override
    public boolean typeMatch(Expression expression) {
        return expression instanceof BinOp &&
                left.typeMatch(((BinOp) expression).left) &&
                op == ((BinOp) expression).op &&
                right.typeMatch(((BinOp) expression).right);
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

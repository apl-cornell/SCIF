package ast;

import compile.SolCode;
import sherrlocUtils.Constraint;
import sherrlocUtils.Inequality;
import sherrlocUtils.Relation;
import typecheck.*;

import java.util.ArrayList;

public class Assign extends NonFirstLayerStatement {
    Expression target;
    Expression value;
    public Assign(Expression target, Expression value) {
        this.target = target;
        this.value = value;
    }

    @Override
    public ScopeContext NTCgenCons(NTCEnv env, ScopeContext parent) {
        ScopeContext now = new ScopeContext(this, parent);
        ScopeContext tgt = target.NTCgenCons(env, now);
        ScopeContext v = value.NTCgenCons(env, now);
        // con: tgt should be a supertype of v
        logger.debug(v);
        logger.debug(env);
        logger.debug(location);
        env.cons.add(tgt.genCons(v, Relation.LEQ, env, location));
        return now;
    }

    @Override
    public Context genConsVisit(VisitEnv env, boolean tail_position) {
        Context context = env.context;
        Context curContext = new Context(context.valueLabelName, Utils.getLabelNameLock(location), context.inLockName);
        // Context prevContext = env.prevContext;

        String ifNamePc = Utils.getLabelNamePc(scopeContext.getSHErrLocName());

        String ifNameTgt = "";
        if (target instanceof Name) {
            //Assuming target is Name
            ifNameTgt = env.getVar(((Name) target).id).labelToSherrlocFmt();
        } else if (target instanceof Subscript || target instanceof Attribute) {
            // env.prevContext = valueContext;
            /*env.cons.add(new Constraint(new Inequality(prevLockName, CompareOperator.Eq, valueContext.lockName), env.hypothesis, value.location, env.curContractSym.name,
                    "Lock should be maintained before execution of this operation"));*/
            Context tmp = target.genConsVisit(env, false);
            // prevContext = tmp;
            ifNameTgt = tmp.valueLabelName;
            // rtnLockName = tmp.lockName;
        } else {
            //TODO: error handling
        }
        env.context = context;
        Context valueContext = value.genConsVisit(env, false);
        String ifNameValue = valueContext.valueLabelName;
        // prevContext = valueContext;

        env.cons.add(new Constraint(new Inequality(ifNameValue, ifNameTgt), env.hypothesis, location, env.curContractSym.name,
                "Integrity of the value being assigned must be trusted to allow this assignment"));

        env.cons.add(new Constraint(new Inequality(ifNamePc, ifNameTgt), env.hypothesis, location, env.curContractSym.name,
                "Integrity of control flow must be trusted to allow this assignment"));

        if (!tail_position) {
            env.cons.add(new Constraint(new Inequality(curContext.lockName, context.inLockName), env.hypothesis, location, env.curContractSym.name,
                    Utils.ERROR_MESSAGE_LOCK_IN_NONLAST_OPERATION));
        } else {
            env.cons.add(new Constraint(new Inequality(curContext.lockName, context.lockName), env.hypothesis, location, env.curContractSym.name,
                    Utils.ERROR_MESSAGE_LOCK_IN_LAST_OPERATION));
        }


        return new Context(ifNameTgt, curContext.lockName, curContext.inLockName);
    }

    public void SolCodeGen(SolCode code) {
        code.addAssign(target.toSolCode(), value.toSolCode());
    }
    @Override
    public ArrayList<Node> children() {
        ArrayList<Node> rtn = new ArrayList<>();
        rtn.add(target);
        rtn.add(value);
        return rtn;
    }
}

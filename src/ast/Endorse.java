package ast;

import typecheck.sherrlocUtils.Constraint;
import typecheck.sherrlocUtils.Inequality;
import typecheck.*;

import java.util.ArrayList;

public class Endorse extends Expression {

    Expression value;
    IfLabel from, to;

    public Endorse(Expression value, IfLabel from, IfLabel to) {
        this.value = value;
        this.from = from;
        this.to = to;
    }

    public ScopeContext ntcGenCons(NTCEnv env, ScopeContext parent) {
        return value.ntcGenCons(env, parent);
    }

    @Override
    public ExpOutcome genConsVisit(VisitEnv env, boolean tail_position) {
        //TODO: change to conditioned form
        Context beginContext = env.inContext;
        Context endContext = new Context(typecheck.Utils.getLabelNamePc(location),
                typecheck.Utils.getLabelNameLock(location));
        ExpOutcome vo = value.genConsVisit(env, tail_position);
        String ifNameValue = vo.valueLabelName;
        String ifNameRtn = scopeContext.getSHErrLocName() + "." + "endorse" + location.toString();

        String fromLabel = from.toSherrlocFmt();
        String toLabel = to.toSherrlocFmt();

        env.cons.add(
                new Constraint(new Inequality(ifNameValue, fromLabel), env.hypothesis, location,
                        env.curContractSym.name,
                        "The expression must be trusted to be endorsed"));

        env.cons.add(new Constraint(new Inequality(toLabel, ifNameRtn), env.hypothesis, location,
                env.curContractSym.name,
                "The integrity level of this expression would be endorsed"));

        //env.outContext = endContext;

        env.trustCons.add(
                new Constraint(new Inequality(beginContext.pc, endContext.pc), env.hypothesis,
                        location, env.curContractSym.name,
                        "The control flow of this expression would be endorsed"));

        return new ExpOutcome(ifNameRtn, vo.psi);
    }

    @Override
    public String toSolCode() {
        return value.toSolCode();
    }

    @Override
    public boolean typeMatch(Expression expression) {
        return expression instanceof Endorse &&
                value.typeMatch(((Endorse) expression).value) &&
                from.typeMatch(((Endorse) expression).from) &&
                to.typeMatch(((Endorse) expression).to);
    }

    @Override
    public ArrayList<Node> children() {
        ArrayList<Node> rtn = new ArrayList<>();
        rtn.add(value);
        rtn.add(from);
        rtn.add(to);
        return rtn;
    }

}

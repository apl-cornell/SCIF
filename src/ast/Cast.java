package ast;

import compile.CompileEnv;
import compile.ast.Statement;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import typecheck.Context;
import typecheck.ExpOutcome;
import typecheck.InterfaceSym;
import typecheck.NTCEnv;
import typecheck.PathOutcome;
import typecheck.PsiUnit;
import typecheck.ScopeContext;
import typecheck.Sym;
import typecheck.TypeSym;
import typecheck.Utils;
import typecheck.VisitEnv;
import typecheck.exceptions.SemanticException;
import typecheck.sherrlocUtils.Relation;

/**
 * A conversion of a value to a contract type or a primitive type, written {@code T(e)}. The
 * parser cannot tell it from a call, so the regular type check resolves a {@link Call} to it.
 */
public class Cast extends Expression {

    private final Name type;
    private final Expression arg;

    /** The cast of {@code arg} to the type named {@code type}. */
    Cast(Name type, Expression arg) {
        this.type = type;
        this.arg = arg;
    }

    @Override
    public ScopeContext genTypeConstraints(NTCEnv env, ScopeContext parent)
            throws SemanticException {
        ScopeContext now = new ScopeContext(this, parent);
        arg.genTypeConstraints(env, now);
        Sym typeSym = env.getCurSym(type.id);
        env.addCons(now.genTypeConstraints(typeSym.getName(), Relation.EQ, env, location));
        return now;
    }

    /**
     * The converted value has the argument's label. A reference to a contract is also trusted
     * no more than the contract it names, which is the principal the argument holds, or not at
     * all if that principal is unknown.
     */
    @Override
    protected ExpOutcome genFlowConstraints(VisitEnv env, boolean tail_position)
            throws SemanticException {
        Context beginContext = env.inContext;
        Context endContext = new Context(Utils.getLabelNamePc(toSHErrLocFmt()),
                Utils.getLabelNameLock(toSHErrLocFmt()));
        PathOutcome psi = new PathOutcome(new PsiUnit(endContext));
        ExpOutcome ao = arg.genIFConstraints(env, false);
        psi.joinExe(ao.psi);
        env.inContext = Utils.genNewContextAndConstraints(env, false,
                ao.psi.getNormalPath().c, beginContext.lambda, arg.nextPcSHL(), arg.location);
        String label = ao.valueLabelName;
        if (env.containsContract(type.id)) {
            String principal = arg.principalName(env);
            label = principal != null ? Utils.joinLabels(label, principal)
                    : env.curContractSym().any().toSHErrLocFmt();
        }
        Utils.contextFlow(env, psi.getNormalPath().c, endContext, arg.location);
        if (!tail_position) {
            env.checkFlow(psi.getNormalPath().c.lambda, beginContext.lambda, location,
                    Utils.ERROR_MESSAGE_LOCK_IN_NONLAST_OPERATION);
        }
        return new ExpOutcome(label, psi);
    }

    @Override
    protected TypeSym staticType(VisitEnv env) {
        InterfaceSym contract = env.getContract(type.id);
        return contract != null ? contract : (TypeSym) env.getCurSym(type.id);
    }

    /** A cast to a contract or address type holds the principal its argument holds. */
    @Override
    public String principalName(VisitEnv env) {
        if (env.containsContract(type.id) || type.id.equals(Utils.ADDRESS_TYPE)) {
            return arg.principalName(env);
        }
        return null;
    }

    @Override
    public compile.ast.Expression solidityCodeGen(List<Statement> result, CompileEnv code) {
        return new compile.ast.Call(type.id, List.of(arg.solidityCodeGen(result, code)));
    }

    @Override
    public boolean typeMatch(Expression expression) {
        return expression instanceof Cast c && type.typeMatch(c.type) && arg.typeMatch(c.arg);
    }

    @Override
    public Map<String, compile.ast.Type> readMap(CompileEnv code) {
        return arg.readMap(code);
    }

    @Override
    public List<Node> children() {
        List<Node> rtn = new ArrayList<>();
        rtn.add(arg);
        return rtn;
    }
}

package ast;

import compile.CompileEnv;
import compile.ast.Statement;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import typecheck.Context;
import typecheck.ExpOutcome;
import typecheck.NTCEnv;
import typecheck.PathOutcome;
import typecheck.PsiUnit;
import typecheck.ScopeContext;
import typecheck.StructTypeSym;
import typecheck.TypeSym;
import typecheck.Utils;
import typecheck.VarSym;
import typecheck.VisitEnv;
import typecheck.exceptions.SemanticException;
import typecheck.sherrlocUtils.Relation;

/**
 * A struct value built from its members, written {@code new S(args)}. The parser cannot tell
 * it from the creation of a contract, so the regular type check resolves a {@link New} to it.
 */
public class StructConstruction extends Expression {

    private final Name struct;
    private final List<Expression> args;

    /** The value of the struct type named {@code struct} with members {@code args}. */
    StructConstruction(Name struct, List<Expression> args) {
        this.struct = struct;
        this.args = args;
    }

    @Override
    public ScopeContext genTypeConstraints(NTCEnv env, ScopeContext parent)
            throws SemanticException {
        ScopeContext now = new ScopeContext(this, parent);
        StructTypeSym structSym = (StructTypeSym) env.getCurSym(struct.id);
        if (args.size() != structSym.merberSize()) {
            throw new SemanticException("number of arguments does not match the "
                    + "number of members of struct " + structSym.getName(), location);
        }
        for (int i = 0; i < args.size(); ++i) {
            Expression arg = args.get(i);
            TypeSym memberType = structSym.getMemberVarInfo(struct.id, i).typeSym;
            ScopeContext argContext = arg.genTypeConstraints(env, now);
            env.addCons(argContext.genTypeConstraints(memberType.toSHErrLocFmt(), Relation.GEQ,
                    env, arg.location));
        }
        env.addCons(now.genTypeConstraints(structSym.toSHErrLocFmt(), Relation.EQ, env,
                location));
        return now;
    }

    /**
     * Each member is checked like an argument of a call, against the member's label. The
     * struct value itself is trusted as much as the control flow creating it.
     */
    @Override
    protected ExpOutcome genFlowConstraints(VisitEnv env, boolean tail_position)
            throws SemanticException {
        Context beginContext = env.inContext;
        Context endContext = new Context(Utils.getLabelNamePc(toSHErrLocFmt()),
                Utils.getLabelNameLock(toSHErrLocFmt()));
        StructTypeSym structSym = (StructTypeSym) env.getCurSym(struct.id);
        PathOutcome psi = new PathOutcome(new PsiUnit(endContext));
        for (int i = 0; i < args.size(); ++i) {
            Expression arg = args.get(i);
            ExpOutcome ao = arg.genIFConstraints(env, false);
            psi.joinExe(ao.psi);
            env.inContext = new Context(
                    Utils.joinLabels(ao.psi.getNormalPath().c.pc, beginContext.pc),
                    beginContext.lambda);
            VarSym member = structSym.members().get(i);
            String memberLabel = member.ifl.toSHErrLocFmt();
            env.checkFlow(ao.valueLabelName, memberLabel, arg.location,
                    "Input to member " + member.getName() + " must be trusted at its label");
            env.checkFlow(env.inContext.pc, memberLabel, arg.location,
                    "Current control flow must be trusted to set member " + member.getName());
        }
        return new ExpOutcome(env.inContext.pc, psi);
    }

    @Override
    protected TypeSym staticType(VisitEnv env) {
        return (StructTypeSym) env.getCurSym(struct.id);
    }

    @Override
    public compile.ast.Expression solidityCodeGen(List<Statement> result, CompileEnv code) {
        List<compile.ast.Expression> argExps = new ArrayList<>();
        for (Expression arg : args) {
            argExps.add(arg.solidityCodeGen(result, code));
        }
        return new compile.ast.Call(struct.id, argExps);
    }

    @Override
    public boolean typeMatch(Expression expression) {
        if (!(expression instanceof StructConstruction c) || !struct.typeMatch(c.struct)
                || args.size() != c.args.size()) {
            return false;
        }
        for (int i = 0; i < args.size(); ++i) {
            if (!args.get(i).typeMatch(c.args.get(i))) {
                return false;
            }
        }
        return true;
    }

    @Override
    public Map<String, compile.ast.Type> readMap(CompileEnv code) {
        Map<String, compile.ast.Type> result = new HashMap<>();
        for (Expression arg : args) {
            result.putAll(arg.readMap(code));
        }
        return result;
    }

    @Override
    public List<Node> children() {
        return new ArrayList<>(args);
    }
}

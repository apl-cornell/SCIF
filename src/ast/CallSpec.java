package ast;

import compile.CompileEnv;
import compile.ast.Statement;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import typecheck.BuiltInT;
import typecheck.Context;
import typecheck.ExpOutcome;
import typecheck.NTCEnv;
import typecheck.PathOutcome;
import typecheck.ScopeContext;
import typecheck.Utils;
import typecheck.VisitEnv;
import typecheck.exceptions.SemanticException;
import typecheck.sherrlocUtils.Constraint;
import typecheck.sherrlocUtils.Inequality;
import typecheck.sherrlocUtils.Relation;

public class CallSpec extends Node {
    Map<String, Expression> specs;

    public CallSpec(Map<String, Expression> specs) {
        assert specs != null;
        this.specs = specs;
    }

    @Override
    public ScopeContext genTypeConstraints(NTCEnv env, ScopeContext parent) throws SemanticException {
        // all expressions should be of uint;

        ScopeContext now = new ScopeContext(this, parent);
        for (Entry<String, Expression> entry: specs.entrySet()) {
            String keyword = entry.getKey();
            if (!keyword.equals(Utils.LABEL_PAYVALUE) && !keyword.equals(Utils.BUILT_IN_GAS)) {
                throw new SemanticException("unsupported call option: " + keyword,
                        entry.getValue().location);
            }
            ScopeContext value = entry.getValue().genTypeConstraints(env, now);
            env.addCons(
                    new Constraint(new Inequality(env.getSymName(BuiltInT.UINT), Relation.LEQ, value.getSHErrLocName()),
                    env.globalHypothesis(), location, "A call option's value must be a uint"));
        }
        return now;
    }

    public PathOutcome genIFConstraints(VisitEnv env, boolean tail_position) throws SemanticException {
        Context beginContext = env.inContext;
        Context endContext = new Context(typecheck.Utils.getLabelNamePc(toSHErrLocFmt()),
                typecheck.Utils.getLabelNameLock(toSHErrLocFmt()));
        // Context prevContext = env.prevContext;

        String ifNamePc = Utils.getLabelNamePc(scopeContext.getSHErrLocName());

        ExpOutcome to = null;
        String ifNameTgt = env.thisSym().labelNameSLC();
        for (Entry<String, Expression> entry: specs.entrySet()) {
            Expression value = entry.getValue();
            ExpOutcome vo = value.genIFConstraints(env, false);
            String ifNameValue = vo.valueLabelName;

            env.checkFlow(ifNameValue, ifNameTgt, value.location,
                    "The value of a call option must be trusted by this contract");

            env.checkFlow(ifNamePc, ifNameTgt, value.location,
                    "Control flow must be trusted by this contract to set a call option");
            typecheck.Utils.contextFlow(env, vo.psi.getNormalPath().c, endContext, value.location);
            // env.outContext = endContext;

            if (!tail_position) {
                env.checkFlow(endContext.lambda, beginContext.lambda, location,
                        typecheck.Utils.ERROR_MESSAGE_LOCK_IN_NONLAST_OPERATION);
            }
            if (to != null) {
                vo.psi.join(to.psi);
            } else {
                to = vo;
            }
        }
        to.psi.setNormalPath(endContext);
        // prevContext = valueContext;


        assert to.psi.getNormalPath().c != null;
        return to.psi;

    }

    @Override
    public List<Node> children() {
//        return List.of(specs);
        return new ArrayList<>(specs.values());
    }

    public compile.ast.CallSpec solidityCodeGen(List<Statement> result, CompileEnv code) {
        Map<String, compile.ast.Expression> specs = new HashMap<>();
        for (Entry<String, Expression> entry: this.specs.entrySet()) {
            specs.put(entry.getKey(), entry.getValue().solidityCodeGen(result, code));
        }
        return new compile.ast.CallSpec(specs);
    }

    /** The local variables the expressions in this spec read, with their types. */
    public Map<String, compile.ast.Type> readMap(CompileEnv code) {
        Map<String, compile.ast.Type> result = new HashMap<>();
        for (Expression spec : specs.values()) {
            result.putAll(spec.readMap(code));
        }
        return result;
    }
}

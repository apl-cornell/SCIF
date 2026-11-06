package ast;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import typecheck.CodeLocation;
import typecheck.Context;
import typecheck.ExpOutcome;
import typecheck.Label;
import typecheck.NTCEnv;
import typecheck.PathOutcome;
import typecheck.ScopeContext;
import typecheck.TypeSym;
import typecheck.Utils;
import typecheck.VarSym;
import typecheck.VisitEnv;
import typecheck.exceptions.SemanticException;
import typecheck.sherrlocUtils.Relation;

/**
 * Checks of the arguments passed for declared parameters, as by a method call, a closure
 * invocation, a throw, or an emit.
 */
final class CallArguments {

    private CallArguments() {
    }

    /**
     * Type-checks {@code args} in scope {@code now}, generating constraints that each has a
     * subtype of the corresponding type in {@code paramTypes}.
     *
     * @param what names what takes the parameters, for error messages, such as "event E"
     * @throws SemanticException if there are not as many arguments as parameters, or an
     *         argument does not type-check
     */
    static void genTypeConstraints(List<Expression> args, List<TypeSym> paramTypes,
            String what, NTCEnv env, ScopeContext now, CodeLocation location)
            throws SemanticException {
        if (args.size() != paramTypes.size()) {
            throw new SemanticException(what + " takes " + paramTypes.size()
                    + " arguments, but " + args.size() + " were given", location);
        }
        for (int i = 0; i < args.size(); ++i) {
            Expression arg = args.get(i);
            ScopeContext argContext = arg.genTypeConstraints(env, now);
            env.addCons(argContext.genTypeConstraints(paramTypes.get(i).toSHErrLocFmt(),
                    Relation.GEQ, env, arg.location));
        }
    }

    /**
     * The result of evaluating arguments.
     *
     * @param labels the SHErrLoc name of the label of each argument's value
     * @param principals the SHErrLoc name of the principal each argument holds, or null
     *        where it holds none or which one is unknown
     */
    record Evaluated(List<String> labels, List<String> principals) {
    }

    /**
     * Generates the information-flow constraints for evaluating {@code args} from left to
     * right, starting in {@code env.inContext}, joining their outcomes into {@code psi} as
     * {@link PathOutcome#joinExe} does and leaving {@code env.inContext} as the context after
     * the last one.
     */
    static Evaluated evaluate(List<Expression> args, VisitEnv env, PathOutcome psi)
            throws SemanticException {
        String lock = env.inContext.lambda;
        List<String> labels = new ArrayList<>();
        List<String> principals = new ArrayList<>();
        for (Expression arg : args) {
            ExpOutcome ao = arg.genIFConstraints(env, false);
            psi.joinExe(ao.psi);
            labels.add(ao.valueLabelName);
            principals.add(arg.principalName(env));
            env.inContext = Utils.genNewContextAndConstraints(env, false,
                    ao.psi.getNormalPath().c, lock, arg.nextPcSHL(), arg.location);
        }
        return new Evaluated(labels, principals);
    }

    /**
     * Adds to {@code mapping}, for each principal-typed parameter in {@code params}, a
     * mapping from its SHErrLoc name to the principal its argument holds, or to
     * {@code unknown} if that is unknown. {@code params} may contain nulls, which stand for
     * parameters that are not principals, and may be shorter than the arguments.
     */
    static void mapPrincipals(List<VarSym> params, Evaluated evaluated, String unknown,
            Map<String, String> mapping) {
        for (int i = 0; i < params.size(); ++i) {
            VarSym param = params.get(i);
            if (param != null && param.isPrincipalTyped()) {
                String principal = evaluated.principals().get(i);
                mapping.put(param.toSHErrLocFmt(), principal != null ? principal : unknown);
            }
        }
    }

    /**
     * Generates constraints that each argument in {@code args}, and the control flow at
     * {@code pc} that passes it, is trusted at the corresponding label in
     * {@code paramLabels} with the principals in it renamed by {@code mapping}.
     *
     * @param of describes what takes the parameters in error messages, such as
     *        " of the event", or is empty
     */
    static void checkFlowInto(List<Expression> args, Evaluated evaluated, String pc,
            List<Label> paramLabels, Map<String, String> mapping, String of, VisitEnv env)
            throws SemanticException {
        for (int i = 0; i < args.size(); ++i) {
            CodeLocation location = args.get(i).location;
            String paramLabel = paramLabels.get(i).toSHErrLocFmt(mapping);
            String nth = Utils.ordNumString(i + 1);
            env.checkFlow(evaluated.labels().get(i), paramLabel, location,
                    "Input to the " + nth + " argument" + of + " must be trusted enough");
            env.checkFlow(pc, paramLabel, location,
                    "Current control flow must be trusted to feed the " + nth + " argument"
                            + of);
        }
    }
}

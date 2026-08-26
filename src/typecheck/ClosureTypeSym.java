package typecheck;

import compile.ast.PrimitiveType;
import compile.ast.Type;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;
import typecheck.sherrlocUtils.Inequality;
import typecheck.sherrlocUtils.Relation;

/**
 * Type symbol for a closure value.
 *
 * The symbol name is purely structural — two closures with the same arity
 * and the same positional parameter/return types share a name, so SHErrLoc
 * treats them as the same type regardless of where each was written
 * (regular typecheck is label-agnostic).
 *
 * For information-flow checking it also carries the closure's labels:
 * the unbound-parameter labels, the external/internal pc, the lock
 * (gamma), and the return label. The closure value's *value* label is
 * not carried here — it lives on the wrapping {@code LabeledType} /
 * {@code VarSym} like every other type's value label. These IFC labels
 * are populated when a declared closure type is resolved; a closure
 * type built only for structural regular typecheck (e.g. a creation
 * expression result) leaves them null.
 */
public class ClosureTypeSym extends TypeSym {
    public final List<TypeSym> unboundParams;
    public final TypeSym returnType;
    public final List<TypeSym> exceptionTypes;

    public List<Label> paramLabels;   // labels of the unbound params, positional
    public Label pcEx, pcIn, gamma, retLabel;
    // closure value label lives in LabeledType instead of here

    // Binder syms of the named final unbound params, positional (null =
    // unnamed slot). Binder names are sugar over positions: rendering
    // maps each sym to its positional token, so alpha-variant closure
    // types produce identical constraints.
    private List<VarSym> binderSyms = new ArrayList<>();

    /** For regular typecheck only. Labels are all set to {@code null}.  */
    public ClosureTypeSym(List<TypeSym> unboundParams, TypeSym returnType,
            List<TypeSym> exceptionTypes, ScopeContext defContext) {
        this(unboundParams, returnType, exceptionTypes, defContext,
                null, null, null, null, null);
    }

    /** For IFC typecheck with labels. */
    public ClosureTypeSym(List<TypeSym> unboundParams, TypeSym returnType,
            List<TypeSym> exceptionTypes, ScopeContext defContext,
            List<Label> paramLabels, Label pcEx, Label pcIn, Label gamma,
            Label retLabel) {
        super(getClosureName(unboundParams, returnType), defContext);
        this.unboundParams = unboundParams;
        this.returnType = returnType;
        this.exceptionTypes = exceptionTypes;
        this.paramLabels = paramLabels == null ? new ArrayList<>() : paramLabels;
        this.pcEx = pcEx;
        this.pcIn = pcIn;
        this.gamma = gamma;
        this.retLabel = retLabel;
    }

    /**
     * Two closure types share a name iff their unbound parameters and return 
     * value match in type names. Labels are not included.
     * 
     */
    public static String getClosureName(List<TypeSym> unboundParams, TypeSym returnType) {
        StringBuilder sb = new StringBuilder("Closure.").append(unboundParams.size());
        for (TypeSym p : unboundParams) {
            sb.append('.').append(p.getName());
        }
        sb.append(".->.").append(returnType.getName());
        return sb.toString();
    }

    public int unboundArity() {
        return unboundParams.size();
    }

    // Label accessors.
    public Label pcEx()      { return pcEx; }
    public Label pcIn()      { return pcIn; }
    public Label callGamma() { return gamma; }
    public Label endPc()     { return retLabel; }
    public Label getLabelArg(int i) { return paramLabels.get(i); }

    /** Canonical rendering namespace of binder tokens; `closure` is a
     *  keyword, so user code cannot collide with it. */
    public static final String BINDER_TOKEN_PREFIX = "closure.arg";

    /** Canonical rendering of the i-th unbound slot's binder. */
    public static String binderToken(int i) {
        return BINDER_TOKEN_PREFIX + i;
    }

    public List<VarSym> binderSyms() {
        return binderSyms;
    }

    public void setBinderSyms(List<VarSym> binderSyms) {
        this.binderSyms = binderSyms;
    }

    /** Register this type's binder tokens as rigid solver atoms
     *  (idempotent; called wherever the type is used with an env). */
    public void registerAtoms(VisitEnv env) {
        for (int i = 0; i < binderSyms.size(); ++i) {
            if (binderSyms.get(i) != null) {
                env.addRigidAtom(binderToken(i));
            }
        }
    }

    /** True when any of this type's labels mentions the given binder.
     *  (Binders shadow outer names inside the type, so a name match on
     *  this type's own labels is exact.) */
    public boolean referencesBinder(VarSym binder) {
        List<Label> all = new ArrayList<>(paramLabels);
        all.add(pcEx);
        all.add(gamma);
        all.add(retLabel);
        for (Label l : all) {
            if (l != null && l.principalLeaves().contains(binder.getName())) {
                return true;
            }
        }
        return false;
    }

    /** {@code map} extended with this type's binder-to-token entries;
     *  entries of {@code map} win (an invoke site substitutes actuals
     *  for tokens' slots). */
    private Map<String, String> withBinderTokens(Map<String, String> map) {
        Map<String, String> merged = new java.util.HashMap<>();
        for (int i = 0; i < binderSyms.size(); ++i) {
            if (binderSyms.get(i) != null) {
                merged.put(binderSyms.get(i).toSHErrLocFmt(), binderToken(i));
            }
        }
        merged.putAll(map);
        return merged;
    }

    // Stable SHErrLoc names for this closure's labels.
    public String pcExSLC()  { return toSHErrLocFmt() + ".extpc"; }
    public String pcInSLC()  { return toSHErrLocFmt() + ".inpc"; }
    public String gammaSLC() { return toSHErrLocFmt() + ".gamma"; }
    public String retSLC()   { return toSHErrLocFmt() + ".returnV"; }

    /** Re-resolve every IFC label of this closure type against {@code env}. */
    public void resolveLabels(VisitEnv env, ast.ClosureType ast)
            throws typecheck.exceptions.SemanticException {
        // Binders are in scope for every label of this type — the
        // dependent-map key pattern. All of them register before any
        // label resolves.
        env.incScopeLayer();
        java.util.List<VarSym> binders = new java.util.ArrayList<>();
        for (int i = 0; i < ast.params.size(); ++i) {
            if (!ast.isBinder(i)) {
                binders.add(null);
                continue;
            }
            VarSym binderVar = env.curContractSym().newVarSym(
                    ast.params.get(i).name(), ast.binderLabeledType(i),
                    false, true, false, ast.params.get(i).location(),
                    new ScopeContext(ast, defContext()));
            try {
                env.addVar(binderVar.getName(), binderVar);
            } catch (SymTab.AlreadyDefined e) {
                throw new typecheck.exceptions.SemanticException(
                        "closure binder name already defined: " + binderVar.getName(),
                        ast.getLocation());
            }
            binders.add(binderVar);
        }
        Label pcExL  = ast.funcLabels == null || ast.funcLabels.begin_pc == null
                ? null : env.toLabel(ast.funcLabels.begin_pc);
        Label pcInL  = ast.funcLabels == null || ast.funcLabels.to_pc == null
                ? null : env.toLabel(ast.funcLabels.to_pc);
        Label gammaL = ast.funcLabels == null || ast.funcLabels.gamma_label == null
                ? null : env.toLabel(ast.funcLabels.gamma_label);
        Label retL   = ast.returnType.label() == null
                ? pcExL : env.toLabel(ast.returnType.label());
        java.util.List<Label> pl = new java.util.ArrayList<>();
        for (ast.LabeledType lt : ast.unboundParams) {
            pl.add(lt.label() == null ? pcExL : env.toLabel(lt.label()));
        }
        env.decScopeLayer();
        this.pcEx = pcExL;
        this.pcIn = pcInL;
        this.gamma = gammaL;
        this.retLabel = retL;
        this.paramLabels = pl;
        this.binderSyms = binders;
        registerAtoms(env);
    }

    /** This closure type's labels, each rendered through {@code map}
     *  (binder occurrences render as their positional tokens unless
     *  {@code map} substitutes them). */
    public RealLabels renderLabels(Map<String, String> map) {
        Map<String, String> merged = withBinderTokens(map);
        List<String> params = new ArrayList<>();
        for (Label l : paramLabels) {
            params.add(RealLabels.render(l, merged));
        }
        return new RealLabels(RealLabels.render(pcEx, merged),
                RealLabels.render(pcIn, merged), RealLabels.render(gamma, merged),
                RealLabels.render(retLabel, merged), params);
    }

    public static class RealLabels {
        public final String pcEx, pcIn, gamma, retLabel;
        /** labels of the still-unbound params, positional */
        public final List<String> paramLabels;

        public RealLabels(String pcEx, String pcIn, String gamma, String retLabel,
                List<String> paramLabels) {
            this.pcEx = pcEx;
            this.pcIn = pcIn;
            this.gamma = gamma;
            this.retLabel = retLabel;
            this.paramLabels = paramLabels == null ? new ArrayList<>() : paramLabels;
        }

        /** {@code l} rendered through {@code map}; null stays null. */
        public static String render(Label l, Map<String, String> map) {
            return l == null ? null : l.toSHErrLocFmt(map);
        }

        // TODO:
        public void emitSubtypeConstraints(ClosureTypeSym target,
                Map<String, String> targetMap, Consumer<Inequality> emit) {
            // Binder occurrences in the target's labels render as their
            // positional tokens, matching the source's rendering.
            targetMap = target.withBinderTokens(targetMap);
            if (pcEx != null && target.pcEx != null) {
                // pc_ex contravariant (decision 2026-07-19, supersedes the
                // D-P2 equality): the believed external label may be
                // stronger than the real one, never weaker. pc_ex is an
                // entry requirement, so it varies like a parameter; the
                // one-sided runtime protocol (believed => real at the
                // closure entry) checks the same direction.
                emit.accept(new Inequality(
                        target.pcEx.toSHErrLocFmt(targetMap), Relation.LEQ, pcEx));
            }
            // pc_2 dropped
            // if (pcIn != null && target.pcIn != null) {
            //     // pc_in covariant
            //     emit.accept(new Inequality(
            //             pcIn, Relation.LEQ, target.pcIn.toSHErrLocFmt(targetMap)));
            // }
            if (gamma != null && target.gamma != null) {
                // gamma contravariant
                emit.accept(new Inequality(
                        target.gamma.toSHErrLocFmt(targetMap), Relation.LEQ, gamma));
            }
            if (retLabel != null && target.retLabel != null) {
                // return label covariant
                emit.accept(new Inequality(
                        retLabel, Relation.LEQ, target.retLabel.toSHErrLocFmt(targetMap)));
            }
            if (paramLabels.size() != target.paramLabels.size()) {
                throw new IllegalStateException(
                        "closure parameter arity mismatch in subtype check: "
                                + paramLabels.size() + " vs " + target.paramLabels.size());
            }
            for (int i = 0; i < paramLabels.size(); i++) {
                String src = paramLabels.get(i);
                Label tgt = target.paramLabels.get(i);
                if (src == null || tgt == null) {
                    continue;
                }
                // param label contravariant
                emit.accept(new Inequality(
                        tgt.toSHErrLocFmt(targetMap), Relation.LEQ, src));
            }
        }
    }

    @Override
    public Type getType() {
        return new PrimitiveType("Closure");
    }
}
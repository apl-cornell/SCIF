package ast;

import compile.CompileEnv;
import compile.ast.PrimitiveType;
import java.util.ArrayList;
import java.util.List;
import typecheck.NTCEnv;
import typecheck.ScopeContext;
import typecheck.SymTab;
import typecheck.TypeSym;
import typecheck.VarSym;
import typecheck.exceptions.SemanticException;

/**
 * A first-class closure type:
 *   ( fun {pc_ex >> pc_in; L} (unboundParams) -> returnType throws ex )
 *
 * The value label of a closure-typed declaration lives on the wrapping
 * {@code LabeledType} (the outer {l_val} written by the user), exactly
 * like every other type. Default values for that label come from the
 * surrounding declaration site (state vars / locals / exception args /
 * event args default to {this}; function parameters default to the
 * enclosing method's pc_ex).
 *
 * An unbound parameter may be named (`final address from`); the name is
 * a binder usable in this type's own labels, like a dependent map's key
 * name. Binder names are sugar over positions: two closure types of the
 * same shape differing only in binder names are the same type.
 */
public class ClosureType extends Type {

    public final FuncLabels funcLabels;
    /** The written unbound-parameter entries; unnamed entries hold a
     *  null name. */
    public final List<Arg> params;
    /** The entries' labeled types, positional with {@code params}. */
    public final List<LabeledType> unboundParams;
    public final LabeledType returnType;
    public final List<LabeledType> exceptionTypes;

    public ClosureType(FuncLabels funcLabels, List<Arg> params,
            LabeledType returnType, List<LabeledType> exceptionTypes) {
        super("$closure");
        this.funcLabels = funcLabels;
        this.params = params == null ? new ArrayList<>() : params;
        this.unboundParams = new ArrayList<>();
        for (Arg p : this.params) {
            this.unboundParams.add(p.annotation);
        }
        this.returnType = returnType;
        this.exceptionTypes = exceptionTypes == null ? new ArrayList<>() : exceptionTypes;
    }

    /** True when the i-th unbound slot has a referable binder: a named,
     *  final entry (only principals may appear in labels). */
    public boolean isBinder(int i) {
        return params.get(i).name() != null && params.get(i).isFinal();
    }

    /** The i-th binder's type with an inert value label, for registering
     *  the binder as a variable (the dependent-map key pattern: the
     *  binder's own value label is separate from the slot's label). */
    public LabeledType binderLabeledType(int i) {
        return new LabeledType(unboundParams.get(i).type(),
                new PrimitiveIfLabel(new Name(typecheck.Utils.LABEL_BOTTOM)));
    }

    @Override
    public boolean isPrimitive() {
        return false;
    }

    @Override
    public String toSolCode() {
        return "Closure";
    }

    @Override
    public compile.ast.Type solidityCodeGen(CompileEnv code) {
        code.markClosureStructRequired();
        return new PrimitiveType("Closure");
    }

    @Override
    public boolean typeMatch(Type expression) {
        if (!(expression instanceof ClosureType other)) return false;
 
        if (unboundParams.size() != other.unboundParams.size()) return false;
        for (int i = 0; i < unboundParams.size(); i++) {
            Type a = unboundParams.get(i).type();
            Type b = other.unboundParams.get(i).type();
            if (!a.typeMatch(b)) return false;
        }

        if (!returnType.type().typeMatch(other.returnType.type())) return false;

        if (exceptionTypes.size() != other.exceptionTypes.size()) return false;
        for (int i = 0; i < exceptionTypes.size(); i++) {
            if (!exceptionTypes.get(i).type().typeMatch(other.exceptionTypes.get(i).type())) {
                return false;
            }
        }

        return (funcLabels == null && other.funcLabels == null)
                || (funcLabels != null && other.funcLabels != null
                        && funcLabels.typeMatch(other.funcLabels));
    }

    @Override
    public ScopeContext genTypeConstraints(NTCEnv env, ScopeContext parent)
            throws SemanticException {
        ScopeContext now = new ScopeContext(this, parent);

        // Binders are in scope for every label written inside this type
        // (the dependent-map key pattern). All of them register before
        // any label resolves, so a label may reference a binder declared
        // to its right.
        env.enterNewScope();
        for (int i = 0; i < params.size(); ++i) {
            if (isBinder(i)) {
                Arg p = params.get(i);
                VarSym binderVar = env.newVarSym(p.name(), binderLabeledType(i),
                        false, true, false, p.location, now);
                try {
                    env.addSym(p.name(), binderVar);
                } catch (SymTab.AlreadyDefined e) {
                    throw new SemanticException(
                            "closure binder name already defined: " + p.name(), p.location);
                }
            }
        }

        for (LabeledType lt : unboundParams) {
            lt.genTypeConstraints(env, parent);
        }

        returnType.genTypeConstraints(env, parent);

        for (LabeledType lt : exceptionTypes) {
            lt.genTypeConstraints(env, parent);
        }

        if (funcLabels != null) {
            funcLabels.genTypeConstraints(env, now);
        }
        env.exitNewScope();

        TypeSym typeSym = env.toTypeSym(this, scopeContext);
        env.addCons(now.genEqualCons(typeSym, env, location, "Type error: Improper closure type"));
        return now;
    }

    @Override
    public List<Node> children() {
        List<Node> rtn = new ArrayList<>();
        if (funcLabels != null) rtn.add(funcLabels);
        rtn.addAll(unboundParams);
        if (returnType != null) rtn.add(returnType);
        rtn.addAll(exceptionTypes);
        return rtn;
    }
}

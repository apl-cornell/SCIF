package ast;

import compile.CompileEnv;
import compile.ast.Statement;
import compile.ast.Type;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import typecheck.exceptions.SemanticException;
import typecheck.sherrlocUtils.Relation;
import typecheck.*;

import java.util.ArrayList;

/**
 * Represent an expression value[index].
 * value is of type map or array, and index's type should be right accordingly.
 */
public class Subscript extends TrailerExpr {

    Expression index;

    public Subscript(Expression v, Expression i) {
        value = v;
        index = i;
    }

    @Override
    public VarSym getVarInfo(NTCEnv env) throws SemanticException {
        VarSym valueVarSym = value.getVarInfo(env);
        VarSym subscriptVarSym = null;
        if (valueVarSym.typeSym instanceof MapTypeSym) {

            subscriptVarSym = new VarSym(
                    valueVarSym.getName() + ".sub",
                    ((MapTypeSym) valueVarSym.typeSym).valueType,
                    null,
                    location,
                    valueVarSym.defContext(),
                    false,
                    false,
                    true
            );
        } else if (valueVarSym.typeSym instanceof ArrayTypeSym) {
            subscriptVarSym = new VarSym(
                valueVarSym.getName() + ".sub",
                    ((ArrayTypeSym) valueVarSym.typeSym).valueType,
                    null,
                    location,
                    valueVarSym.defContext(),
                    false,
                    false,
                    true
            );
        }
        return subscriptVarSym;
    }

    @Override
    public ScopeContext genTypeConstraints(NTCEnv env, ScopeContext parent) throws SemanticException {
        ScopeContext now = new ScopeContext(this, parent);
        VarSym valueVarSym = value.getVarInfo(env);
        ScopeContext idx = index.genTypeConstraints(env, now);
        value.genTypeConstraints(env, now);

        if (valueVarSym.typeSym instanceof DepMapTypeSym) {
            // The index must be a variable holding a principal, which the element's label
            // may name.
            if (!(index instanceof Name && index.getVarInfo(env) instanceof VarSym indexSym
                    && indexSym.isPrincipalTyped())) {
                throw new SemanticException(
                        "the index of a dependent map must be an address or contract variable",
                        index.location);
            }
            return now;
        } else if (valueVarSym.typeSym instanceof MapTypeSym) {
            MapTypeSym typeInfo = (MapTypeSym) valueVarSym.typeSym;
            // index matches the keytype
            env.addCons(idx.genTypeConstraints(typeInfo.keyType.getName(), Relation.LEQ, env, location, "A map index must have the map's key type"));
            // valueType matches the result exp
            env.addCons(now.genTypeConstraints(typeInfo.valueType.getName(), Relation.EQ, env, location, "A map element has the map's value type"));
            return now;
        } else if (valueVarSym.typeSym instanceof ArrayTypeSym) {
            ArrayTypeSym typeInfo = (ArrayTypeSym) valueVarSym.typeSym;
            env.addCons(idx.genTypeConstraints(Utils.BuiltinType2ID(BuiltInT.UINT), Relation.LEQ, env, location, "An array index must be a uint"));
            env.addCons(now.genTypeConstraints(typeInfo.valueType.getName(), Relation.EQ, env, location, "An array element has the array's element type"));
            return now;
        } else {
            throw new SemanticException("only maps and arrays can be subscripted", value.location);
        }
    }

    @Override
    protected ExpOutcome genFlowConstraints(VisitEnv env, boolean tail_position) throws SemanticException {
        Context beginContext = env.inContext;
        Context endContext = new Context(typecheck.Utils.getLabelNamePc(toSHErrLocFmt()),
                typecheck.Utils.getLabelNameLock(toSHErrLocFmt()));

        Map<String, String> dependentMapping = new HashMap<>();
        ExpOutcome vo = value.genIFConstraints(env, false);
        TypeSym collectionType = value.type();

        String ifNameValue = vo.valueLabelName;
        // String ifNameRtnValue = ifNameValue + "." + "Subscript" + location.toString();
        String ifNameRtnValue = toSHErrLocFmt();
        ExpOutcome io = index.genIFConstraints(env, tail_position);


        // String ifNameRtnLock = "";
        if (collectionType instanceof DepMapTypeSym depMapTypeSym) {
            String elementLabel = elementLabel(depMapTypeSym, indexPrincipal(env));
            // Unless the element's label names its key, the index chooses which element is
            // read, so the result is trusted no more than the index.
            return new ExpOutcome(namesKey(depMapTypeSym) ? elementLabel
                    : Utils.joinLabels(elementLabel, io.valueLabelName), io.psi);
        } else {
            Context indexContext = io.psi.getNormalPath().c;
            String ifNameIndex = io.valueLabelName;
            //ifNameRtnValue =
            //        scopeContext.getSHErrLocName() + "." + "Subscript" + location.toString();
            // The index chooses which element is read, so the result is trusted no more than it.
            env.checkEqual(Utils.joinLabels(ifNameValue, ifNameIndex), ifNameRtnValue, location,
                    "An element is trusted no more than the collection and the index used to read it");

            // env.cons.add(new Constraint(new Inequality(ifNameIndex, ifNameRtnValue), env.hypothesis, location));
            // ifNameRtnLock = indexContext.lambda;
            return new ExpOutcome(ifNameRtnValue, io.psi);
        }
    }

    @Override
    public compile.ast.Expression solidityCodeGen(List<Statement> result, CompileEnv code) {
        return new compile.ast.Subscript(value.solidityCodeGen(result, code), index.solidityCodeGen(result, code));
    }

    @Override
    public VarSym getVarInfo(VisitEnv env, boolean tail_position, Map<String, String> dependentMapping)
            throws SemanticException {
        VarSym rtnVarSym = null;
        VarSym valueVarSym = value.getVarInfo(env, false, dependentMapping);
        String ifNameValue = valueVarSym.labelNameSLC();
//        String ifNameRtn = ifNameValue + "." + "Subscript" + location.toString();
        String ifNameRtn = scopeContext.getSHErrLocName() + "." + "Subscript" + location.toString();
        if (valueVarSym.typeSym instanceof DepMapTypeSym depMapTypeSym) {
            TypeSym rtnTypeSym = depMapTypeSym.valueType;
            dependentMapping.put(depMapTypeSym.key().toSHErrLocFmt(), indexPrincipal(env));
            rtnVarSym = new VarSym(ifNameRtn, rtnTypeSym, valueVarSym.ifl, location,
                    rtnTypeSym.defContext(), false, false, false);
            env.checkFlow(elementLabel(depMapTypeSym, indexPrincipal(env)),
                    rtnVarSym.labelNameSLC(), location, "An element is trusted no more than the collection it is read from");
            if (!namesKey(depMapTypeSym)) {
                env.checkFlow(index.genIFConstraints(env, tail_position).valueLabelName,
                        rtnVarSym.labelNameSLC(), location, "An element is trusted no more than the index used to read it");
            }
        } else {
            String ifNameIndex = index.genIFConstraints(env, tail_position).valueLabelName;

            TypeSym rtnTypeSym = valueVarSym.typeSym instanceof ArrayTypeSym arrayType
                    ? arrayType.valueType : ((MapTypeSym) valueVarSym.typeSym).valueType;
            rtnVarSym = new VarSym(ifNameRtn, rtnTypeSym, valueVarSym.ifl, location,
                    valueVarSym.defContext(), false, false, false);
            env.checkFlow(ifNameValue, rtnVarSym.labelNameSLC(), location,
                    "An element is trusted no more than the collection it is read from");

            env.checkFlow(ifNameIndex, rtnVarSym.labelNameSLC(), location,
                    "An element is trusted no more than the index used to read it");

        }
        return rtnVarSym;
    }

//    @Override
//    public String toSolCode() {
//        String i = index.toSolCode();
//        String v = value.toSolCode();
//        return v + "[" + i + "]";
//    }

    @Override
    public ArrayList<Node> children() {
        ArrayList<Node> rtn = new ArrayList<>();
        rtn.add(value);
        rtn.add(index);
        return rtn;
    }

    @Override
    public boolean typeMatch(Expression expression) {
        return expression instanceof Subscript &&
                super.typeMatch(expression) &&
                index.typeMatch(((Subscript) expression).index);
    }

    @Override
    public java.util.Map<String, compile.ast.Type> readMap(CompileEnv code) {
        Map<String, Type> result = index.readMap(code);
        result.putAll(value.readMap(code));
        return result;
    }

    @Override
    public Map<String,? extends Type> writeMap(CompileEnv code) {
        return value.writeMap(code);
    }

    @Override
    protected TypeSym staticType(VisitEnv env) {
        TypeSym collectionType = value.type();
        if (collectionType instanceof ArrayTypeSym arrayType) {
            return arrayType.valueType;
        } else if (collectionType instanceof MapTypeSym mapType) {
            return mapType.valueType;
        }
        throw new AssertionError("subscript of " + collectionType.getName());
    }

    @Override
    protected ExpOutcome genLocationFlow(VisitEnv env) throws SemanticException {
        ExpOutcome bo = value.genLocationConstraints(env);
        TypeSym collectionType = value.type();
        ExpOutcome io = index.genIFConstraints(env, false);
        PathOutcome psi = io.psi;
        psi.join(bo.psi.withoutNormalPath());
        if (collectionType instanceof DepMapTypeSym depMapTypeSym) {
            String elementLabel = elementLabel(depMapTypeSym, indexPrincipal(env));
            // If the element's label names its key, it is trusted by the principal the index
            // holds, whoever chose it. Otherwise the index chooses which element is
            // overwritten, so it must be trusted at the element's label.
            if (!namesKey(depMapTypeSym)) {
                env.checkFlow(io.valueLabelName, elementLabel, index.location,
                        "The index of an assignment target must be trusted at the label of the element");
            }
            return new ExpOutcome(elementLabel, psi);
        }
        // The index chooses which element is overwritten, so it must be trusted as much as
        // the collection.
        env.checkFlow(io.valueLabelName, bo.valueLabelName, index.location,
                "The index of an assignment target must be trusted at the label of the collection");
        return new ExpOutcome(bo.valueLabelName, psi);
    }

    /**
     * The principal the index of this subscript holds, which must be a principal-typed
     * variable.
     */
    private String indexPrincipal(VisitEnv env) throws SemanticException {
        String principal = index.principalName(env);
        if (principal == null) {
            throw new SemanticException(
                    "the index of a dependent map must be an address or contract variable",
                    index.location);
        }
        return principal;
    }

    /** The label of the element of a map of type {@code type} whose key holds {@code key}. */
    private static String elementLabel(DepMapTypeSym type, String key) {
        Map<String, String> mapping = new HashMap<>();
        mapping.put(type.key().toSHErrLocFmt(), key);
        return type.label().toSHErrLocFmt(mapping);
    }

    /** Whether the label of the elements of a map of type {@code type} names its key. */
    private static boolean namesKey(DepMapTypeSym type) {
        String key = type.key().toSHErrLocFmt();
        return type.label().principalSyms().stream()
                .anyMatch(p -> p.toSHErrLocFmt().equals(key));
    }
}

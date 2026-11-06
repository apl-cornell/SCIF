package ast;

import compile.CompileEnv;
import compile.ast.Attr;
import compile.ast.Statement;
import compile.ast.Type;

import java.util.List;
import java.util.Map;

import typecheck.exceptions.SemanticException;
import typecheck.sherrlocUtils.Relation;
import typecheck.*;

import java.util.ArrayList;

public class Attribute extends TrailerExpr {

    // Set at regular typechecking when this node is `c.target` on a
    // closure value; drives the value-label rule and codegen.
    private boolean isClosureTarget = false;

    /**
     * value attr
     */
    Name attr;

    // value.attr
    public Attribute(Expression v, Name a) {
        value = v;
        attr = a;
    }

    // steph: if this represents "a.b", then value = a and attr = b.

    @Override
    public ScopeContext genTypeConstraints(NTCEnv env, ScopeContext parent) throws SemanticException {
        VarSym varSym = getVarInfo(env);
        ScopeContext now = new ScopeContext(this, parent);
        env.addCons(now.genTypeConstraints(varSym.typeSym.getName(), Relation.EQ, env, location));
        return now;
    }

    /**
     * Returns the member variable this attribute denotes.
     *
     * @throws SemanticException if the value is not a variable, or its type
     *         has no member with this name
     */
    @Override
    public VarSym getVarInfo(NTCEnv env) throws SemanticException {
        VarSym rtnVarSym;
        VarSym parentVarSym = value.getVarInfo(env);
        if (parentVarSym == null) {
            throw new SemanticException("not a variable: "
                    + (value instanceof Name n ? n.id : "expression"), value.location);
        }
        if (parentVarSym.typeSym instanceof ClosureTypeSym
                && attr.id.equals(Utils.CLOSURE_TARGET_MEMBER)) {
            // c.target: the closure's target address, read-only.
            this.isClosureTarget = true;
            return new VarSym(Utils.CLOSURE_TARGET_MEMBER,
                    (TypeSym) env.getSym(BuiltInT.ADDRESS), null, location,
                    parentVarSym.defContext(), false, true, true);
        }
        if (parentVarSym.typeSym instanceof StructTypeSym) {
            StructTypeSym parentTypeInfo = (StructTypeSym) parentVarSym.typeSym;
            rtnVarSym = parentTypeInfo.getMemberVarInfo(parentVarSym.toSHErrLocFmt(), attr.id);
        } else if (parentVarSym.typeSym instanceof ExceptionTypeSym) {
            ExceptionTypeSym parentTypeInfo = (ExceptionTypeSym) parentVarSym.typeSym;
            rtnVarSym = parentTypeInfo.getMemberVarInfo(parentVarSym.toSHErrLocFmt(), attr.id);
        } else if (parentVarSym.typeSym instanceof EventTypeSym) {
            // TODO steph double check--not sure of correctness
            EventTypeSym parentTypeInfo = (EventTypeSym) parentVarSym.typeSym;
            rtnVarSym = parentTypeInfo.getMemberVarInfo(parentVarSym.toSHErrLocFmt(), attr.id);
        }
        else {
            throw new SemanticException("type " + parentVarSym.typeSym.getName()
                    + " has no members", location);
        }
        if (rtnVarSym == null) {
            throw new SemanticException("no member named " + attr.id + " in type "
                    + parentVarSym.typeSym.getName(), attr.location);
        }
        return rtnVarSym;
    }

    @Override
    protected ExpOutcome genFlowConstraints(VisitEnv env, boolean tail_position) throws SemanticException {
        ExpOutcome vo = value.genIFConstraints(env, tail_position);
        TypeSym baseType = value.type();
        if (baseType instanceof ClosureTypeSym && attr.id.equals(Utils.CLOSURE_TARGET_MEMBER)) {
            return vo;
        }
        // A member is trusted at its label and no more than the value it belongs to.
        String attrValueLabel = vo.valueLabelName;
        if (baseType instanceof StructTypeSym structType) {
            attrValueLabel = Utils.joinLabels(attrValueLabel,
                    memberLabel(env, structType.member(attr.id).ifl, attrValueLabel));
        } else if (baseType instanceof ExceptionTypeSym expType) {
            // Each principal parameter in the label stands for the principal in that field.
            java.util.Map<String, String> mapping = new java.util.HashMap<>();
            for (VarSym param : expType.parameters()) {
                if (param.isPrincipalTyped()) {
                    mapping.put(param.toSHErrLocFmt(), value instanceof Name n
                            ? exceptionFieldPrincipal(env, env.getVar(n.id), param.getName())
                            : env.curContractSym().any().toSHErrLocFmt());
                }
            }
            VarSym member = expType.getMemberVarInfo("", attr.id);
            attrValueLabel = Utils.joinLabels(attrValueLabel, member.ifl.toSHErrLocFmt(mapping));
        } else {
            throw new AssertionError("no member " + attr.id + " in type " + baseType.getName());
        }
        String ifNameRnt = scopeContext.getSHErrLocName() + ".struct" + location.toString();
        env.checkEqual(attrValueLabel, ifNameRnt, location,
                "Integrity of the index value must be trusted to indicate the attribute");
        return new ExpOutcome(ifNameRnt, vo.psi);
    }

    @Override
    public String principalName(VisitEnv env) {
        if (value instanceof Name n && env.getCurSym(n.id) instanceof VarSym v
                && v.typeSym instanceof ExceptionTypeSym expType) {
            for (VarSym param : expType.parameters()) {
                if (param.getName().equals(attr.id) && param.isPrincipalTyped()) {
                    return exceptionFieldPrincipal(env, v, attr.id);
                }
            }
        }
        return null;
    }

    /**
     * The label {@code label} of a struct member, as this contract reads it in a struct value
     * labeled {@code structLabel}. A struct declared by another contract labels its members in
     * that contract's terms: its {@code this} is trusted only as much as the value, its top and
     * bottom principals are this contract's, and any other principal it names is unknown here.
     */
    private static String memberLabel(VisitEnv env, Label label, String structLabel) {
        String own = env.thisSym().toSHErrLocFmt();
        String namespace = own.substring(0, own.length() - Utils.LABEL_THIS.length());
        java.util.Map<String, String> mapping = new java.util.HashMap<>();
        for (VarSym p : label.principalSyms()) {
            String name = p.toSHErrLocFmt();
            if (name.startsWith(namespace)) {
                continue;
            }
            mapping.put(name, switch (p.getName()) {
                case Utils.LABEL_THIS -> structLabel;
                case Utils.LABEL_TOP -> env.curContractSym().top().toSHErrLocFmt();
                default -> env.curContractSym().any().toSHErrLocFmt();
            });
        }
        return label.toSHErrLocFmt(mapping);
    }

    /**
     * The principal held by the principal-typed field {@code field} of the caught exception
     * {@code exception}, registered in {@code env}. Nothing is known about it.
     */
    private static String exceptionFieldPrincipal(VisitEnv env, VarSym exception, String field) {
        return env.freshPrincipal(exception.toSHErrLocFmt() + "." + field);
    }

    @Override
    public compile.ast.Expression solidityCodeGen(List<Statement> result, CompileEnv code) {
        if (isClosureTarget) {
            return new Attr(value.solidityCodeGen(result, code), "addr");
        }
        return new Attr(value.solidityCodeGen(result, code), attr.id);
    }

    @Override
    public VarSym getVarInfo(VisitEnv env, boolean tail_position, Map<String, String> dependentMapping)
            throws SemanticException {
        VarSym rtnVarSym;
        VarSym parentVarSym = value.getVarInfo(env, tail_position, dependentMapping);
        StructTypeSym parentTypeInfo = (StructTypeSym) parentVarSym.typeSym;
        rtnVarSym = parentTypeInfo.getMemberVarInfo(parentVarSym.toSHErrLocFmt(), attr.id);
        return rtnVarSym;
    }

    public boolean isGlobalStruct(NTCEnv env) {
        if (value instanceof Name) {
            Sym parentVarSym = env.getCurSym(((Name) value).id);
            return parentVarSym.isGlobal();
        }
        return false;
    }

//    public String toSolCode() {
//        return CompileEnv.toAttribute(value.toSolCode(), attr.id);
//    }

    @Override
    public ArrayList<Node> children() {
        ArrayList<Node> rtn = new ArrayList<>();
        rtn.add(value);
        return rtn;
    }

    @Override
    public boolean typeMatch(Expression expression) {
        return expression instanceof Attribute &&
                super.typeMatch(expression) &&
                attr.typeMatch(((Attribute) expression).attr);
    }

    @Override
    public java.util.Map<String, compile.ast.Type> readMap(CompileEnv code) {
        return value.readMap(code);
    }

    @Override
    public Map<String,? extends Type> writeMap(CompileEnv code) {
        return value.writeMap(code);
    }

    @Override
    protected TypeSym staticType(VisitEnv env) {
        TypeSym base = value.type();
        if (base instanceof ClosureTypeSym && attr.id.equals(Utils.CLOSURE_TARGET_MEMBER)) {
            return env.builtinType(BuiltInT.ADDRESS);
        } else if (base instanceof StructTypeSym structType) {
            return structType.member(attr.id).typeSym;
        } else if (base instanceof ExceptionTypeSym expType) {
            return expType.getMemberVarInfo("", attr.id).typeSym;
        }
        throw new AssertionError("no member " + attr.id + " in type " + base.getName());
    }

    @Override
    protected ExpOutcome genLocationFlow(VisitEnv env) throws SemanticException {
        ExpOutcome bo = value.genLocationConstraints(env);
        TypeSym baseType = value.type();
        String location = bo.valueLabelName;
        if (baseType instanceof StructTypeSym structType) {
            location = Utils.joinLabels(location,
                    memberLabel(env, structType.member(attr.id).ifl, bo.valueLabelName));
        } else if (baseType instanceof ExceptionTypeSym expType) {
            java.util.Map<String, String> mapping = new java.util.HashMap<>();
            for (VarSym param : expType.parameters()) {
                if (param.isPrincipalTyped()) {
                    mapping.put(param.toSHErrLocFmt(), value instanceof Name n
                            ? exceptionFieldPrincipal(env, env.getVar(n.id), param.getName())
                            : env.curContractSym().any().toSHErrLocFmt());
                }
            }
            location = Utils.joinLabels(location,
                    expType.getMemberVarInfo("", attr.id).ifl.toSHErrLocFmt(mapping));
        } else {
            throw new SemanticException("member " + attr.id + " cannot be assigned", this.location);
        }
        return new ExpOutcome(location, bo.psi);
    }
}

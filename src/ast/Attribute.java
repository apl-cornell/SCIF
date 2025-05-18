package ast;

import compile.CompileEnv;
import compile.ast.Attr;
import compile.ast.Statement;
import compile.ast.Type;

import java.util.List;
import java.util.Map;
import typecheck.sherrlocUtils.Constraint;
import typecheck.sherrlocUtils.Inequality;
import typecheck.sherrlocUtils.Relation;
import typecheck.*;

import java.util.ArrayList;

public class Attribute extends TrailerExpr {

    /**
     * value attr
     */
    Name attr;

    // value.attr
    public Attribute(Expression v, Name a) {
        value = v;
        attr = a;
    }

    @Override
    public ScopeContext genTypeConstraints(NTCEnv env, ScopeContext parent) {
        VarSym varSym = getVarInfo(env);
        assert varSym != null: "attribute value not found: " + location.errString();
        ScopeContext now = new ScopeContext(this, parent);
        env.addCons(now.genTypeConstraints(varSym.typeSym.getName(), Relation.EQ, env, location));
        return now;
    }

    public VarSym getVarInfo(NTCEnv env) {
        VarSym rtnVarSym;
        VarSym parentVarSym = value.getVarInfo(env);
        assert parentVarSym != null: "at " + location.errString();
        if (parentVarSym.typeSym instanceof StructTypeSym) {
            StructTypeSym parentTypeInfo = (StructTypeSym) parentVarSym.typeSym;
            rtnVarSym = parentTypeInfo.getMemberVarInfo(parentVarSym.toSHErrLocFmt(), attr.id);
        } else if (parentVarSym.typeSym instanceof ExceptionTypeSym) {
            ExceptionTypeSym parentTypeInfo = (ExceptionTypeSym) parentVarSym.typeSym;
            rtnVarSym = parentTypeInfo.getMemberVarInfo(parentVarSym.toSHErrLocFmt(), attr.id);
        } else {
            assert false: "unknown types for attribute: " + value.toString() + " at " + location.errString();
            return null;
        }
        return rtnVarSym;
    }

    @Override
    public ExpOutcome genIFConstraints(VisitEnv env, boolean tail_position) {
        //TODO: assuming only one-level attribute access
        // to add support to multi-level access
        String varName = ((Name) value).id;

        VarSym varSym = env.getVar(varName);
        if (!(varSym.typeSym instanceof StructTypeSym structType)) {
            //TODO: throw errors: variable not struct
//            return null;
            throw new RuntimeException("Variable not a struct: " + varName);
        }

        //String prevLockName = env.prevContext.lambda;
        ExpOutcome vo = value.genIFConstraints(env, tail_position);
        String attrValueLabel = vo.valueLabelName;
//        String ifAttLabel = structType.getMemberLabel(attr.id);
        String ifNameRnt = scopeContext.getSHErrLocName() + ".struct" + location.toString();
//        env.cons.add(new Constraint(new Inequality(ifNameRnt, ifAttLabel), env.hypothesis(), location,
//                env.curContractSym().getName(),
//                "Integrity of the member"));
//        if (!ifAttLabel.equals(attrValueLabel)) {
            env.cons.add(new Constraint(new Inequality(attrValueLabel, Relation.EQ, ifNameRnt), env.hypothesis(),
                    location, env.curContractSym().getName(),
                    "Integrity of the index value must be trusted to indicate the attribute"));
//        }

        return new ExpOutcome(ifNameRnt, vo.psi);
    }

    @Override
    public compile.ast.Expression solidityCodeGen(List<Statement> result, CompileEnv code) {
        return new Attr(value.solidityCodeGen(result, code), attr.id);
    }

    @Override
    public VarSym getVarInfo(VisitEnv env, boolean tail_position, Map<String, String> dependentMapping) {
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
}

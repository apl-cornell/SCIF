package ast;

import compile.SolCode;
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
    public ScopeContext ntcGenCons(NTCEnv env, ScopeContext parent) {
        VarSym varSym = getVarInfo(env);
        ScopeContext now = new ScopeContext(this, parent);
        env.addCons(now.genCons(varSym.typeSym.getName(), Relation.EQ, env, location));
        return now;
    }

    public VarSym getVarInfo(NTCEnv env) {
        VarSym rtnVarSym;
        VarSym parentVarSym = value.getVarInfo(env);
        StructTypeSym parentTypeInfo = (StructTypeSym) parentVarSym.typeSym;
        rtnVarSym = parentTypeInfo.getMemberVarInfo(parentVarSym.toSHErrLocFmt(), attr.id);
        return rtnVarSym;
    }

    @Override
    public ExpOutcome genConsVisit(VisitEnv env, boolean tail_position) {
        //TODO: assuming only one-level attribute access
        // to add support to multi-level access
        String varName = ((Name) value).id;
        if (!env.curContractSym.containVar(varName)) {
            //TODO: throw errors: variable not found
            return null;
        }
        VarSym varSym = env.getVar(varName);
        if (!(varSym.typeSym instanceof StructTypeSym)) {
            //TODO: throw errors: variable not struct
            return null;
        }

        StructTypeSym structType = (StructTypeSym) varSym.typeSym;
        //String prevLockName = env.prevContext.lambda;
        ExpOutcome vo = value.genConsVisit(env, tail_position);
        String attrValueLabel = vo.valueLabelName;
        String ifAttLabel = structType.getMemberLabel(attr.id);
        String ifNameRnt = scopeContext.getSHErrLocName() + ".struct" + location.toString();
        env.cons.add(new Constraint(new Inequality(ifNameRnt, ifAttLabel), env.hypothesis(), location,
                env.curContractSym.getName(),
                "Integrity of the member"));
        if (!ifAttLabel.equals(attrValueLabel)) {
            env.cons.add(new Constraint(new Inequality(ifNameRnt, attrValueLabel), env.hypothesis(),
                    location, env.curContractSym.getName(),
                    "Integrity of the index value must be trusted to indicate the attribute"));
        }

        return new ExpOutcome(ifNameRnt, vo.psi);
    }

    public VarSym getVarInfo(VisitEnv env, boolean tail_position) {
        VarSym rtnVarSym;
        VarSym parentVarSym = value.getVarInfo(env, tail_position);
        StructTypeSym parentTypeInfo = (StructTypeSym) parentVarSym.typeSym;
        rtnVarSym = parentTypeInfo.getMemberVarInfo(parentVarSym.toSHErrLocFmt(), attr.id);
        return rtnVarSym;
    }

    public String toSolCode() {
        return SolCode.toAttribute(value.toSolCode(), attr.id);
    }

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
}

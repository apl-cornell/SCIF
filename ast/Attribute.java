package ast;

import compile.SolCode;
import sherrlocUtils.Constraint;
import sherrlocUtils.Inequality;
import sherrlocUtils.Relation;
import typecheck.*;

import java.util.ArrayList;

public class Attribute extends TrailerExpr {
    Name attr;
    public  Attribute(Expression v, Name a) {
        value = v;
        attr = a;
    }

    @Override
    public ScopeContext NTCgenCons(NTCEnv env, ScopeContext parent) {
        VarSym varSym = getVarInfo(env);
        ScopeContext now = new ScopeContext(this, parent);
        env.addCons(now.genCons(varSym.typeSym.name, Relation.EQ, env, location));
        return now;
    }

    public VarSym getVarInfo(NTCEnv env) {
        VarSym rtnVarSym;
        VarSym parentVarSym = value.getVarInfo(env);
        StructTypeSym parentTypeInfo = (StructTypeSym) parentVarSym.typeSym;
        rtnVarSym = parentTypeInfo.getMemberVarInfo(parentVarSym.toSherrlocFmt(), attr.id);
        return rtnVarSym;
    }

    @Override
    public Context genConsVisit(VisitEnv env) {
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
        //String prevLockName = env.prevContext.lockName;
        Context tmp = value.genConsVisit(env);
        String ifAttLabel = structType.getMemberLabel(attr.id);
        String ifNameRnt = scopeContext.getSHErrLocName() + ".struct" + location.toString();
        env.cons.add(new Constraint(new Inequality(ifNameRnt, ifAttLabel), env.hypothesis, location, env.curContractSym.name,
                "Integrity of the member"));
        if (!ifAttLabel.equals(tmp.valueLabelName)) {
            env.cons.add(new Constraint(new Inequality(ifNameRnt, tmp.valueLabelName), env.hypothesis, location, env.curContractSym.name,
                    "Integrity of the index value must be trusted to indicate the attribute"));
        }


        //env.cons.add(new Constraint(new Inequality(prevLockName, CompareOperator.Eq, tmp.lockName), env.hypothesis, location));
        return new Context(ifNameRnt, tmp.lockName);
    }
    public VarSym getVarInfo(VisitEnv env) {
        VarSym rtnVarSym;
        VarSym parentVarSym = value.getVarInfo(env);
        StructTypeSym parentTypeInfo = (StructTypeSym) parentVarSym.typeSym;
        rtnVarSym = parentTypeInfo.getMemberVarInfo(parentVarSym.toSherrlocFmt(), attr.id);
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

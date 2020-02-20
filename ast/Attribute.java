package ast;

import sherrlocUtils.Constraint;
import sherrlocUtils.Inequality;
import typecheck.*;

import java.util.ArrayList;
import java.util.HashMap;

public class Attribute extends TrailerExpr {
    Name attr;
    public  Attribute(Expression v, Name a) {
        value = v;
        attr = a;
    }
    public VarInfo getVarInfo(NTCEnv env) {
        VarInfo rtnVarInfo;
        VarInfo parentVarInfo = value.getVarInfo(env);
        StructType parentTypeInfo = (StructType) parentVarInfo.typeInfo.type;
        rtnVarInfo = parentTypeInfo.getMemberVarInfo(parentVarInfo.fullName, attr.id);
        return rtnVarInfo;
    }
    @Override
    public Context genConsVisit(VisitEnv env) {
        //TODO: assuming only one-level attribute access
        // to add support to multi-level access
        String varName = ((Name) value).id;
        if (!env.contractInfo.varMap.containsKey(varName)) {
            //TODO: throw errors: variable not found
            return null;
        }
        VarInfo varInfo = env.contractInfo.varMap.get(varName);
        if (!(varInfo.typeInfo.type instanceof StructType)) {
            //TODO: throw errors: variable not struct
            return null;
        }

        StructType structType = (StructType) varInfo.typeInfo.type;
        //String prevLockName = env.prevContext.lockName;
        Context tmp = value.genConsVisit(env);
        String ifAttLabel = structType.getMemberLabel(attr.id);
        String ifNameRnt = env.ctxt + ".struct" + location.toString();
        env.cons.add(new Constraint(new Inequality(ifNameRnt, ifAttLabel), env.hypothesis, location));
        if (!ifAttLabel.equals(tmp.valueLabelName)) {
            env.cons.add(new Constraint(new Inequality(ifNameRnt, tmp.valueLabelName), env.hypothesis, location));
        }


        //env.cons.add(new Constraint(new Inequality(prevLockName, CompareOperator.Eq, tmp.lockName), env.hypothesis, location));
        return new Context(ifNameRnt, tmp.lockName);
    }
    public VarInfo getVarInfo(VisitEnv env) {
        VarInfo rtnVarInfo;
        VarInfo parentVarInfo = value.getVarInfo(env);
        StructType parentTypeInfo = (StructType) parentVarInfo.typeInfo.type;
        rtnVarInfo = parentTypeInfo.getMemberVarInfo(parentVarInfo.fullName, attr.id);
        return rtnVarInfo;
    }
}

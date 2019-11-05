package ast;

import sherrlocUtils.Constraint;
import sherrlocUtils.Inequality;
import typecheck.*;

import java.util.ArrayList;
import java.util.HashMap;

public class Subscript extends TrailerExpr {
    Expression index; //TODO: to be slice

    public Subscript(Expression v, Expression i, Context c) {
        value = v;
        index = i;
    }

    @Override
    public Context genConsVisit(VisitEnv env) {
        VarInfo valueVarInfo = value.getVarInfo(env);
        String ifNameValue = valueVarInfo.labelToSherrlocFmt();
        String ifNameRtnValue = ifNameValue + "." + "Subscript" + location.toString();
        String ifNameRtnLock = "";
        if (valueVarInfo.typeInfo instanceof DepMapTypeInfo) {
            VarInfo indexVarInfo = index.getVarInfo(env);
            String ifNameIndex = indexVarInfo.fullName;

            if (indexVarInfo.typeInfo.type.typeName.equals(Utils.ADDRESSTYPE)) {
                logger.debug("typename {} to {}", valueVarInfo.typeInfo.type.typeName, ifNameIndex);
                //System.err.println("typename " + valueVarInfo.type.typeName + " to " + ifNameIndex);
                String ifDepMapIndexReq = ((DepMapTypeInfo) valueVarInfo.typeInfo).keyType.ifl.toSherrlocFmt(valueVarInfo.typeInfo.type.typeName, ifNameIndex);
                String ifDepMapValue = ((DepMapTypeInfo) valueVarInfo.typeInfo).valueType.ifl.toSherrlocFmt(valueVarInfo.typeInfo.type.typeName, ifNameIndex);
                env.cons.add(new Constraint(new Inequality(ifNameIndex + "..lbl", ifDepMapIndexReq), env.hypothesis, location));

                env.cons.add(new Constraint(new Inequality(ifDepMapValue, ifNameRtnValue), env.hypothesis, location));

                ifNameRtnLock = env.prevContext.lockName;
            } else {
                logger.error("non-address type variable as index to access DEPMAP @{}", locToString());
                //System.out.println("ERROR: non-address type variable as index to access DEPMAP @" + locToString());
                return null;
            }
        } else {
            Context indexContext = index.genConsVisit(env);
            String ifNameIndex = indexContext.valueLabelName;
            ifNameRtnValue = env.ctxt + "." + "Subscript" + location.toString();
            env.cons.add(new Constraint(new Inequality(ifNameValue, ifNameRtnValue), env.hypothesis, location));

            env.cons.add(new Constraint(new Inequality(ifNameIndex, ifNameRtnValue), env.hypothesis, location));
            ifNameRtnLock = indexContext.lockName;
        }
        return new Context(ifNameRtnValue, ifNameRtnLock);
    }

    public VarInfo getVarInfo(VisitEnv env) {
        VarInfo rtnVarInfo = null;
        VarInfo valueVarInfo = value.getVarInfo(env);
        String ifNameValue = valueVarInfo.labelToSherrlocFmt();
        String ifNameRtn = ifNameValue + "." + "Subscript" + location.toString();
        if (valueVarInfo.typeInfo instanceof DepMapTypeInfo) {
            VarInfo indexVarInfo = index.getVarInfo(env);
            String ifNameIndex = indexVarInfo.fullName;
            if (indexVarInfo.typeInfo.type.typeName.equals(Utils.ADDRESSTYPE)) {

                TypeInfo rtnTypeInfo = new TypeInfo(((DepMapTypeInfo) valueVarInfo.typeInfo).valueType);
                rtnTypeInfo.replace(valueVarInfo.typeInfo.type.typeName, ifNameIndex);
                rtnVarInfo = new VarInfo(ifNameRtn, ifNameRtn, rtnTypeInfo, location);

                String ifDepMapIndexReq = ((DepMapTypeInfo) valueVarInfo.typeInfo).keyType.ifl.toSherrlocFmt(valueVarInfo.typeInfo.type.typeName, ifNameIndex);
                String ifDepMapValue = ((DepMapTypeInfo) valueVarInfo.typeInfo).valueType.ifl.toSherrlocFmt(valueVarInfo.typeInfo.type.typeName, ifNameIndex);
                env.cons.add(new Constraint(new Inequality(ifNameIndex + "..lbl", ifDepMapIndexReq), env.hypothesis, location));

                env.cons.add(new Constraint(new Inequality(ifDepMapValue, ifNameRtn), env.hypothesis, location));



            } else {
                logger.error("non-address type variable as index to access DEPMAP @{}", locToString());
                //System.out.println("ERROR: non-address type variable as index to access DEPMAP @" + locToString());
                return null;
            }
        } else {
            String ifNameIndex = index.genConsVisit(env).valueLabelName;
            ifNameRtn = env.ctxt + "." + "Subscript" + location.toString();
            env.cons.add(new Constraint(new Inequality(ifNameValue, ifNameRtn), env.hypothesis, location));

            env.cons.add(new Constraint(new Inequality(ifNameIndex, ifNameRtn), env.hypothesis, location));

            TypeInfo rtnTypeInfo = new TypeInfo(new BuiltinType(ifNameRtn), null, false);
            //TODO: more careful thoughts
            rtnVarInfo = new VarInfo(ifNameRtn, ifNameRtn, rtnTypeInfo, location);
        }
        return rtnVarInfo;
    }
}

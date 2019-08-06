package ast;

import sherrlocUtils.Constraint;
import sherrlocUtils.Inequality;
import typecheck.*;

import java.util.ArrayList;
import java.util.HashMap;

public class Subscript extends TrailerExpr {
    Expression index; //TODO: to be slice
    Context ctx;

    public Subscript(Expression v, Expression i, Context c) {
        value = v;
        index = i;
        ctx = c;
    }

    @Override
    public String genConsVisit(VisitEnv env) {
        VarInfo valueVarInfo = value.getVarInfo(env);
        String ifNameValue = valueVarInfo.labelToSherrlocFmt();
        String ifNameRtn = ifNameValue + "." + "Subscript" + location.toString();
        if (valueVarInfo.type instanceof DepMapTypeInfo) {
            VarInfo indexVarInfo = index.getVarInfo(env);
            String ifNameIndex = indexVarInfo.fullName;

            if (indexVarInfo.type.typeName.equals(Utils.ADDRESSTYPE)) {
                System.err.println("typename " + valueVarInfo.type.typeName + " to " + ifNameIndex);
                String ifDepMapIndexReq = ((DepMapTypeInfo) valueVarInfo.type).keyType.ifl.toSherrlocFmt(valueVarInfo.type.typeName, ifNameIndex);
                String ifDepMapValue = ((DepMapTypeInfo) valueVarInfo.type).valueType.ifl.toSherrlocFmt(valueVarInfo.type.typeName, ifNameIndex);
                env.cons.add(new Constraint(new Inequality(ifNameIndex, ifDepMapIndexReq), env.hypothesis, location));

                env.cons.add(new Constraint(new Inequality(ifDepMapValue, ifNameRtn), env.hypothesis, location));

            } else {
                System.out.println("ERROR: non-address type variable as index to access DEPMAP @" + locToString());
                return "";
            }
        } else {
            String ifNameIndex = index.genConsVisit(env);
            ifNameRtn = env.ctxt + "." + "Subscript" + location.toString();
            env.cons.add(new Constraint(new Inequality(ifNameValue, ifNameRtn), env.hypothesis, location));

            env.cons.add(new Constraint(new Inequality(ifNameIndex, ifNameRtn), env.hypothesis, location));

        }
        return ifNameRtn;
    }

    public VarInfo getVarInfo(VisitEnv env) {
        VarInfo rtnVarInfo = null;
        VarInfo valueVarInfo = value.getVarInfo(env);
        String ifNameValue = valueVarInfo.labelToSherrlocFmt();
        String ifNameRtn = ifNameValue + "." + "Subscript" + location.toString();
        if (valueVarInfo.type instanceof DepMapTypeInfo) {
            VarInfo indexVarInfo = index.getVarInfo(env);
            String ifNameIndex = indexVarInfo.fullName;
            if (indexVarInfo.type.typeName.equals(Utils.ADDRESSTYPE)) {

                TypeInfo rtnTypeInfo = new TypeInfo(((DepMapTypeInfo) valueVarInfo.type).valueType);
                rtnTypeInfo.replace(valueVarInfo.type.typeName, ifNameIndex);
                rtnVarInfo = new VarInfo(ifNameRtn, ifNameRtn, rtnTypeInfo, location);

                String ifDepMapIndexReq = ((DepMapTypeInfo) valueVarInfo.type).keyType.ifl.toSherrlocFmt(valueVarInfo.type.typeName, ifNameIndex);
                String ifDepMapValue = ((DepMapTypeInfo) valueVarInfo.type).valueType.ifl.toSherrlocFmt(valueVarInfo.type.typeName, ifNameIndex);
                env.cons.add(new Constraint(new Inequality(ifNameIndex, ifDepMapIndexReq), env.hypothesis, location));

                env.cons.add(new Constraint(new Inequality(ifDepMapValue, ifNameRtn), env.hypothesis, location));



            } else {
                System.out.println("ERROR: non-address type variable as index to access DEPMAP @" + locToString());
                return null;
            }
        } else {
            String ifNameIndex = index.genConsVisit(env);
            ifNameRtn = env.ctxt + "." + "Subscript" + location.toString();
            env.cons.add(new Constraint(new Inequality(ifNameValue, ifNameRtn), env.hypothesis, location));

            env.cons.add(new Constraint(new Inequality(ifNameIndex, ifNameRtn), env.hypothesis, location));

            TypeInfo rtnTypeInfo = new TypeInfo(ifNameRtn, null, false);
            rtnVarInfo = new VarInfo(ifNameRtn, ifNameRtn, rtnTypeInfo, location);
        }
        return rtnVarInfo;
    }
}

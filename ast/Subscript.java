package ast;

import sherrlocUtils.Constraint;
import sherrlocUtils.Inequality;
import sherrlocUtils.Relation;
import typecheck.*;

import java.util.ArrayList;

public class Subscript extends TrailerExpr {
    Expression index; //TODO: to be slice

    public Subscript(Expression v, Expression i, Context c) {
        value = v;
        index = i;
    }

    //TODO: getVarInfo(NTCEnv)
    @Override
    public ScopeContext NTCgenCons(NTCEnv env, ScopeContext parent) {
        ScopeContext now = new ScopeContext(this, parent);
        VarInfo valueVarInfo = value.getVarInfo(env);
        ScopeContext idx = index.NTCgenCons(env, now);
        value.NTCgenCons(env, now);
        //TODO: support DepMap

        if (valueVarInfo.typeInfo instanceof DepMapTypeInfo) {
            return null;
        } else if (valueVarInfo.typeInfo instanceof MapTypeInfo) {
            MapTypeInfo typeInfo = (MapTypeInfo) valueVarInfo.typeInfo;
            // index matches the keytype
            env.addCons(idx.genCons(typeInfo.keyType.type.typeName, Relation.LEQ, env, location));
            // valueType matches the result exp
            env.addCons(now.genCons(typeInfo.valueType.type.typeName, Relation.EQ, env, location));
            return now;
        } else {
            System.err.println("Subscript: value type not found");
            return null;
        }
    }

    @Override
    public Context genConsVisit(VisitEnv env) {
        VarInfo valueVarInfo = value.getVarInfo(env);
        String ifNameValue = valueVarInfo.labelToSherrlocFmt();
        String ifNameRtnValue = ifNameValue + "." + "Subscript" + location.toString();
        String ifNameRtnLock = "";
        if (valueVarInfo.typeInfo instanceof DepMapTypeInfo) {
            VarInfo indexVarInfo = index.getVarInfo(env);
            logger.debug("subscript/DepMap:");
            logger.debug("lookup at: " + index.toString());
            logger.debug(indexVarInfo.toString());
            String ifNameIndex = indexVarInfo.toSherrlocFmt();

            if (indexVarInfo.typeInfo.type.typeName.equals(Utils.ADDRESSTYPE)) {
                logger.debug("typename {} to {}", valueVarInfo.typeInfo.type.typeName, ifNameIndex);
                //System.err.println("typename " + valueVarInfo.type.typeName + " to " + ifNameIndex);
                String ifDepMapIndexReq = ((DepMapTypeInfo) valueVarInfo.typeInfo).keyType.ifl.toSherrlocFmt(valueVarInfo.typeInfo.type.typeName, ifNameIndex);
                String ifDepMapValue = ((DepMapTypeInfo) valueVarInfo.typeInfo).valueType.ifl.toSherrlocFmt(valueVarInfo.typeInfo.type.typeName, ifNameIndex);
                env.cons.add(new Constraint(new Inequality(ifNameIndex + "..lbl", ifDepMapIndexReq), env.hypothesis, location));

                env.cons.add(new Constraint(new Inequality(ifDepMapValue, Relation.EQ, ifNameRtnValue), env.hypothesis, location));

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
            env.cons.add(new Constraint(new Inequality(ifNameValue, Relation.EQ, ifNameRtnValue), env.hypothesis, location));

            // env.cons.add(new Constraint(new Inequality(ifNameIndex, ifNameRtnValue), env.hypothesis, location));
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
            String ifNameIndex = indexVarInfo.toSherrlocFmt();
            if (indexVarInfo.typeInfo.type.typeName.equals(Utils.ADDRESSTYPE)) {

                TypeInfo rtnTypeInfo = new TypeInfo(((DepMapTypeInfo) valueVarInfo.typeInfo).valueType);
                rtnTypeInfo.replace(valueVarInfo.typeInfo.type.typeName, ifNameIndex);
                rtnVarInfo = new VarInfo(ifNameRtn, rtnTypeInfo, location, false);

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
            rtnVarInfo = new VarInfo(ifNameRtn, rtnTypeInfo, location, false);
        }
        return rtnVarInfo;
    }

    @Override
    public String toSolCode() {
        String i = index.toSolCode();
        String v = value.toSolCode();
        return v + "[" + i + "]";
    }
    @Override
    public ArrayList<Node> children() {
        ArrayList<Node> rtn = new ArrayList<>();
        rtn.add(value);
        rtn.add(index);
        return rtn;
    }
}

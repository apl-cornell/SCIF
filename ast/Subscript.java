package ast;

import utils.*;

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
    public String genConsVisit(String ctxt, HashMap<String, FuncInfo> funcMap, ArrayList<IfConstraint> cons, LookupMaps varNameMap) {
        VarInfo valueVarInfo = value.getVarInfo(ctxt, funcMap, cons, varNameMap);
        String ifNameValue = valueVarInfo.varName;
        String ifNameRnt = ifNameValue + "." + "Subscript" + location.toString();
        if (valueVarInfo.type instanceof DepMapTypeInfo) {
            VarInfo indexVarInfo = index.getVarInfo(ctxt, funcMap, cons, varNameMap);
            if (indexVarInfo.type.typeName.equals(Utils.ADDRESSTYPE) && indexVarInfo instanceof TestableVarInfo
                    && ((TestableVarInfo) indexVarInfo).tested) {
                String ifNameIndex = ((TestableVarInfo) indexVarInfo).testedLabel;
                System.err.println("typename " + valueVarInfo.type.typeName + " to " + ifNameIndex);
                String ifDepMapIndexReq = ((DepMapTypeInfo) valueVarInfo.type).keyType.ifl.toSherrlocFmt(valueVarInfo.type.typeName, ifNameIndex);
                String ifDepMapValue = ((DepMapTypeInfo) valueVarInfo.type).valueType.ifl.toSherrlocFmt(valueVarInfo.type.typeName, ifNameIndex);
                cons.add(Utils.genCons(ifNameIndex, ifDepMapIndexReq, location));
                cons.add(Utils.genCons(ifDepMapValue, ifNameRnt, location));
            } else {
                System.err.println("ERROR: untested address as index to access DEPMAP @" + locToString());
                return "";
            }
        } else {
            String ifNameIndex = index.genConsVisit(ctxt, funcMap, cons, varNameMap);
            ifNameRnt = ctxt + "." + "Subscript" + location.toString();
            cons.add(Utils.genCons(ifNameValue, ifNameRnt, location));
            cons.add(Utils.genCons(ifNameIndex, ifNameRnt, location));
        }
        return ifNameRnt;
    }

    public VarInfo getVarInfo(String ctxt, HashMap<String, FuncInfo> funcMap, ArrayList<IfConstraint> cons, LookupMaps varNameMap) {
        VarInfo rtnVarInfo = null;
        VarInfo valueVarInfo = value.getVarInfo(ctxt, funcMap, cons, varNameMap);
        String ifNameValue = valueVarInfo.varName;
        String ifNameRnt = ifNameValue + "." + "Subscript" + location.toString();
        if (valueVarInfo.type instanceof DepMapTypeInfo) {
            VarInfo indexVarInfo = index.getVarInfo(ctxt, funcMap, cons, varNameMap);
            String ifNameIndex = indexVarInfo.varName;
            if (indexVarInfo.type.typeName.equals(Utils.ADDRESSTYPE) && indexVarInfo instanceof TestableVarInfo
                    && ((TestableVarInfo) indexVarInfo).tested) {

                TypeInfo rtnTypeInfo = new TypeInfo(((DepMapTypeInfo) valueVarInfo.type).valueType);
                rtnTypeInfo.replace(valueVarInfo.type.typeName, ifNameIndex);
                rtnVarInfo = new VarInfo(ifNameRnt, rtnTypeInfo, location);

                String ifDepMapIndexReq = ((DepMapTypeInfo) valueVarInfo.type).keyType.ifl.toSherrlocFmt(valueVarInfo.type.typeName, ifNameIndex);
                String ifDepMapValue = ((DepMapTypeInfo) valueVarInfo.type).valueType.ifl.toSherrlocFmt(valueVarInfo.type.typeName, ifNameIndex);
                cons.add(Utils.genCons(ifNameIndex, ifDepMapIndexReq, location));
                cons.add(Utils.genCons(ifDepMapValue, ifNameRnt, location));


            } else {
                System.err.println("ERROR: untested address as index to access DEPMAP @" + locToString());
                return null;
            }
        } else {
            String ifNameIndex = index.genConsVisit(ctxt, funcMap, cons, varNameMap);
            ifNameRnt = ctxt + "." + "Subscript" + location.toString();
            cons.add(Utils.genCons(ifNameValue, ifNameRnt, location));
            cons.add(Utils.genCons(ifNameIndex, ifNameRnt, location));
            TypeInfo rtnTypeInfo = new TypeInfo(ifNameRnt, null, false);
            rtnVarInfo = new VarInfo(ifNameRnt, rtnTypeInfo, location);
        }
        return rtnVarInfo;
    }
}

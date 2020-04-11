package typecheck;

import ast.*;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;

public class ContractInfo {
    public String name;
    public HashSet<String> iptContracts;
    public HashMap<String, Type> typeMap;
    public HashMap<String, VarInfo> varMap;
    public HashMap<String, FuncInfo> funcMap;
    public ArrayList<TrustConstraint> trustCons;

    public ContractInfo(String name, HashSet<String> iptContracts, HashMap<String, Type> typeMap, HashMap<String, VarInfo> varMap, HashMap<String, FuncInfo> funcMap, ArrayList<TrustConstraint> trustCons) {
        this.name = name;
        this.iptContracts = iptContracts;
        this.typeMap = typeMap;
        this.varMap = varMap;
        this.funcMap = funcMap;
        this.trustCons = trustCons;
    }

    public ContractInfo() {
        name = "UNKNOWN";
        iptContracts = new HashSet<>();
        typeMap = new HashMap<>();
        varMap = new HashMap<>();
        funcMap = new HashMap<>();
        trustCons = new ArrayList<>();
    }

    public Type toType(String typeName) {
        if (typeMap.containsKey(typeName))
            return typeMap.get(typeName);
        Type type;
        if (Utils.BUILTIN_TYPES.contains(typeName)) {
            type = new BuiltinType(typeName);
        } else if (iptContracts.contains(typeName)) {
            type = new ContractType(typeName);
        } else {
            // type not found;
            return null;
        }
        typeMap.put(typeName, type);
        return type;
    }

    public Type toStructType(String typeName, ArrayList<AnnAssign> members) {
        if (typeMap.containsKey(typeName))
            return typeMap.get(typeName);
        ArrayList<VarInfo> memberList = new ArrayList<>();
        for (AnnAssign member : members) {
            VarInfo tmp = member.toVarInfo(this);
            memberList.add(tmp);
        }
        return new StructType(typeName, memberList);
    }

    public TypeInfo toTypeInfo(ast.Type astType) {
        if (astType == null) {
            return new TypeInfo(new BuiltinType("void"), null);
        }

        TypeInfo typeInfo = null;
        if (!(astType instanceof LabeledType)) {
            String typeName = astType.x;
            Type type = toType(typeName);
            typeInfo = new TypeInfo(type, null);
        } else {
            LabeledType lt = (LabeledType) astType;
            if (lt instanceof DepMap) {
                DepMap depMap = (DepMap) lt;
                // TODO: fix the naming
                typeInfo = new DepMapTypeInfo(toType("DepMap"), depMap.ifl, toTypeInfo(depMap.keyType), toTypeInfo(depMap.valueType));
            } else if (lt instanceof Map) {
                Map map = (Map) lt;
                typeInfo = new MapTypeInfo(toType("Map"), map.ifl, toTypeInfo(map.keyType), toTypeInfo(map.valueType));
            } else {
                typeInfo = new TypeInfo(toType(lt.x), lt.ifl);
            }
        }
        return typeInfo;
    }


    public VarInfo toVarInfo(String localName, ast.Type astType, boolean isConst, CodeLocation loc) {
        TypeInfo typeInfo = toTypeInfo(astType);
        return new VarInfo(localName, typeInfo, loc, isConst);
    }
}

package typecheck;

import ast.DepMap;
import ast.Expression;
import ast.LabeledType;
import ast.Name;

import java.util.HashMap;
import java.util.HashSet;

public class ContractInfo {
    public String name;
    public HashSet<String> iptContracts;
    public HashMap<String, Type> typeMap;
    public HashMap<String, VarInfo> varMap;
    public HashMap<String, FuncInfo> funcMap;

    public ContractInfo(String name, HashSet<String> iptContracts, HashMap<String, Type> typeMap, HashMap<String, VarInfo> varMap, HashMap<String, FuncInfo> funcMap) {
        this.name = name;
        this.iptContracts = iptContracts;
        this.typeMap = typeMap;
        this.varMap = varMap;
        this.funcMap = funcMap;
    }

    public ContractInfo() {
        name = "UNKNOWN";
        iptContracts = new HashSet<>();
        typeMap = new HashMap<>();
        varMap = new HashMap<>();
        funcMap = new HashMap<>();
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
            type = new StructType(typeName);
        }
        typeMap.put(typeName, type);
        return type;
    }

    public TypeInfo toTypeInfo(ast.Type astType, boolean isConst) {
        if (astType == null) {
            return new TypeInfo(new BuiltinType("void"), null, isConst);
        }

        TypeInfo typeInfo = null;
        if (!(astType instanceof LabeledType)) {
            String typeName = astType.x;
            Type type = toType(typeName);
            typeInfo = new TypeInfo(type, null, isConst);
        } else {
            LabeledType lt = (LabeledType) astType;
            if (lt instanceof DepMap) {
                DepMap depMap = (DepMap) lt;
                typeInfo = new DepMapTypeInfo(toType(lt.x), depMap.ifl, isConst, toTypeInfo(depMap.keyType, isConst), toTypeInfo(depMap.valueType, isConst));
            } else {
                typeInfo = new TypeInfo(toType(lt.x), lt.ifl, isConst);
            }
        }
        return typeInfo;
    }


    public VarInfo toVarInfo(String fullName, String localName, ast.Type astType, boolean isConst, CodeLocation loc) {
        TypeInfo typeInfo = toTypeInfo(astType, isConst);
        return new VarInfo(fullName, localName, typeInfo, loc);
    }
}

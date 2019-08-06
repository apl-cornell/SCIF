package typecheck;

import ast.*;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.util.ArrayList;

public class Utils {
    //public static final String ENDORCE_FUNC_NAME = "endorce";
    public static final String TOP = "TOP";
    public static final String BOTTOM = "BOT";
    public static final String DEAD = "---DEAD---";
    public static final String KEY = "KEY";
    public static final String SHERRLOC_TOP = "TOP";
    public static final String SHERRLOC_BOTTOM = "BOT";

    public static final String SHERRLOC_PASS_INDICATOR = "No errors";
    public static final String SHERRLOC_ERROR_INDICATOR = "wrong";
    public static final String TYPECHECK_PASS_MSG = "The program typechecks.";
    public static final String TYPECHECK_ERROR_MSG = "The program doesn't typecheck.";


    public static final String ADDRESSTYPE = "address";



    public static IfConstraint genCons(String from, String to, CodeLocation location) {
        // right flows to left
        return new IfConstraint("<=", from, to, location);
    }
    public static IfConstraint genNewlineCons() {
        return new IfConstraint();
    }

    public static String getLabelNamePc(String prefix) {
        if (prefix.equals("")) {
            return "PC";
        } else {
            return prefix + ".." + "PC";
        }
    }
    public static String getLabelNameFuncCallBefore(String funcName) {
        return funcName + ".." + "call.before";
    }
    public static String getLabelNameFuncCallAfter(String funcName) {
        return funcName + ".." + "call.after";
    }
    public static String getLabelNameFuncReturn(String funcName) {
        return funcName + ".." + "rtn";
    }
    public static String getLabelNameArgLabel(String funcName, VarInfo arg) {
        return funcName + "." + arg.localName + "..lbl";
    }
    public static String[] runSherrloc(String path, String consFilePath) throws Exception {
        String[] command = new String[] {"bash", "-c", path + "/sherrloc/sherrloc -c " + consFilePath};
        ProcessBuilder pb = new ProcessBuilder(command);
        //pb.inheritIO();
        Process p = pb.start();
        BufferedReader br = new BufferedReader(new InputStreamReader(p.getInputStream()));
        p.waitFor();

        ArrayList<String> list = new ArrayList<>();
        String tmp;
        while ((tmp = br.readLine()) != null) {
            list.add(tmp);
            //System.err.println(tmp);
        }
        return list.toArray(new String[0]);
    }

    public static VarInfo toVarInfo(String fullName, String localName, Expression type, boolean isConst, CodeLocation loc) {
        /*String varName = "";
        if (name instanceof Name) {
            varName = ((Name) name).id;
        } else {
            //TODO
        }*/

        //boolean testable = false;
        TypeInfo typeInfo = toTypeInfo(type, isConst);

        /*if (type instanceof Name) {
            String typeName = ((Name) type).id;
            if (typeName.equals(Utils.ADDRESSTYPE)) {
                testable = true;
            }
        } else if (type instanceof LabeledType) {
            String typeName = ((LabeledType) type).x.id;
            if (typeName.equals(Utils.ADDRESSTYPE)) {
                testable = true;
            }
        }*/

        //System.err.println("creating new VarInfo" + (testable ? "testable" : "nontestable"));
        //System.err.println("VarName: " + varName);
        /*if (testable)
            return new TestableVarInfo(varName, typeInfo, loc, null, false);
        else*/
        return new VarInfo(fullName, localName, typeInfo, loc);
    }

    public static TypeInfo toTypeInfo(Expression type, boolean isConst) {

        TypeInfo typeInfo = null;
        if (type instanceof Name) {
            String typeName = ((Name) type).id;
            typeInfo = new TypeInfo(typeName, null, isConst);
        } else if (type instanceof LabeledType) {
            LabeledType lt = (LabeledType) type;
            if (lt instanceof DepMap) {
                DepMap depMap = (DepMap) lt;
                typeInfo = new DepMapTypeInfo(lt.x.id, depMap.ifl, isConst, toTypeInfo(depMap.keyType, isConst), toTypeInfo(depMap.valueType, isConst));
            } else {
                typeInfo = new TypeInfo(lt.x.id, lt.ifl, isConst);
            }
        } else {
            //TODO: error handling
        }
        return typeInfo;
    }
}


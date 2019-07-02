package utils;

public class Utils {
    public static final String ENDORCE_FUNC_NAME = "endorce";
    public static final String TOP = "TOP";
    public static final String BOTTOM = "BOT";
    public static final String SHERRLOC_TOP = "TOP";
    public static final String SHERRLOC_BOTTOM = "BOT";
    public static IfConstraint genCons(String left, String right, CodeLocation location) {
        // right flows to left
        return new IfConstraint("<=", left, right, location);
    }
    public static IfConstraint genNewlineCons() {
        return new IfConstraint();
    }

    public static String getIfNamePc(String prefix) {
        if (prefix.equals("")) {
            return "PC";
        } else {
            return prefix + ".." + "PC";
        }
    }
    public static String getIfNameFuncCall(String funcName) {
        return funcName + ".." + "call";
    }
    public static String getIfNameFuncReturn(String funcName) {
        return funcName + ".." + "rnt";
    }
    public static String getIfNameArgLabel(String funcName, VarInfo arg) {
        return funcName + "." + arg.varName;
    }
}

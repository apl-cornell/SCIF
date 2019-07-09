package utils;

public class Utils {
    public static final String ENDORCE_FUNC_NAME = "endorce";
    public static final String TOP = "TOP";
    public static final String BOTTOM = "BOT";
    public static final String SHERRLOC_TOP = "TOP";
    public static final String SHERRLOC_BOTTOM = "BOT";
    public static IfConstraint genCons(String to, String from, CodeLocation location) {
        // right flows to left
        return new IfConstraint("<=", to, from, location);
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
        return funcName + ".." + "rnt";
    }
    public static String getLabelNameArgLabel(String funcName, VarInfo arg) {
        return funcName + "." + arg.varName;
    }
    public static void runSherrloc(String path, String consFilePath) throws Exception {
        String[] command = new String[] {"bash", "-c", path + "/sherrloc/sherrloc -c " + consFilePath};
        ProcessBuilder pb = new ProcessBuilder(command);
        pb.inheritIO();
        Process p = pb.start();
        p.waitFor();
    }
}

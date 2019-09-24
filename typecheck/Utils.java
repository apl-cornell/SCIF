package typecheck;

import ast.*;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;

public class Utils {
    public static final String[] BUILTIN_TYPE_NAMES = new String[] {"bool", "int128", "uint256", "address", "bytes", "string", "int", "map"};
    public static final HashSet<String> BUILTIN_TYPES = new HashSet<>(Arrays.asList(BUILTIN_TYPE_NAMES));

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
    public static final String TYPECHECK_NORESULT_MSG = "No result from ShErrLoc.";


    public static final String ADDRESSTYPE = "address";



    /*public static IfConstraint genCons(String from, String to, CodeLocation location) {
        // right flows to left
        return new IfConstraint("<=", from, to, location);
    }*/
    /*public static IfConstraint genNewlineCons() {
        return new IfConstraint();
    }*/

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


    protected static final Logger logger = LogManager.getLogger();

}


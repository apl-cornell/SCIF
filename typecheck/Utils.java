package typecheck;

import ast.*;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import sherrlocUtils.Constraint;

import java.io.*;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;

public class Utils {
    public static final String[] BUILTIN_TYPE_NAMES = new String[] {"bool", "int128", "uint256", "address", "bytes", "string", "int", "void", "uint"};
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
    public static final String PUBLIC_DECORATOR = "public";
    public static final String PAYABLE_DECORATOR = "payable";


    public static final boolean isPrimitiveType(String x) {
        return BUILTIN_TYPES.contains(x);
    }

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
    public static String getLabelNameLock(String prefix) {
        if (prefix.equals("")) {
            return "LK";
        } else {
            return prefix + ".." + "LK";
        }
    }
    public static String getLabelNameFuncCallPc(String funcName) {
        return funcName + ".." + "call.pc";
    }
    public static String getLabelNameFuncCallLock(String funcName) {
        return funcName + ".." + "call.lk";
    }
    /*public static String getLabelNameFuncCallBefore(String funcName) {
        return funcName + ".." + "call.before";
    }
    public static String getLabelNameFuncCallAfter(String funcName) {
        return funcName + ".." + "call.after";
    }*/
    public static String getLabelNameFuncRtnValue(String funcName) {
        return funcName + ".." + "rtn.v";
    }
    public static String getLabelNameFuncRtnLock(String funcName) {
        return funcName + ".." + "rtn.lk";
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

    public static String joinLabels(String lhs, String rhs) {
        return "(" + lhs + " ⊔ " + rhs + ")";
    }
    public static String meetLabels(String lhs, String rhs) {
        return "(" + lhs + " ⊓ " + rhs + ")";
    }

    public static String BuiltinType2ID(BuiltInT type) {
        if (type == BuiltInT.UINT)
            return "uint";
        else if (type == BuiltInT.BOOL)
            return "bool";
        else if (type == BuiltInT.STRING)
            return "string";
        else if (type == BuiltInT.VOID)
            return "void";
        else if (type == BuiltInT.ADDRESS)
            return "address";
        else
            return "unknownT";
    }

    public static void writeCons2File(HashSet<String> constructors, ArrayList<Constraint> assumptions, ArrayList<Constraint> constraints, File outputFile) {
        try {
            BufferedWriter consFile = new BufferedWriter(new FileWriter(outputFile));
            logger.debug("Writing the constraints of size {}", constraints.size());
            //System.err.println("Writing the constraints of size " + env.cons.size());
            if (!constructors.isEmpty()) {
                for (String principal : constructors) {
                    consFile.write("CONSTRUCTOR " + principal + " 0\n");
                }
            }
            if (!assumptions.isEmpty()) {
                consFile.write("%%\n");
                for (Constraint con : assumptions) {
                    consFile.write(con.toSherrlocFmt(false) + "\n");
                }
                consFile.write("%%\n");
            } else {
                consFile.write("\n");
            }
            if (!constraints.isEmpty()) {
                for (Constraint con : constraints) {
                    consFile.write(con.toSherrlocFmt(true) + "\n");
                }
            }
            consFile.close();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    protected static final Logger logger = LogManager.getLogger();

    public static FuncInfo getCurrentFuncInfo(NTCEnv env, NTCContext now) {
        while (!(now.cur instanceof FunctionDef)) {
            now = now.parent;
        }
        FunctionDef funcNode = (FunctionDef) now.cur;
        Sym sym = env.getCurSym(funcNode.name);
        return ((FuncSym) sym).funcInfo;
    }

    public static TypeInfo getBuiltinTypeInfo(String typeName, SymTab s) {
        return new TypeInfo(((TypeSym) s.lookup(typeName)).type, null, false);
    }

    public static VarInfo createVarInfo(String fullName, String localName, String typeName, SymTab s) {
        return new VarInfo(fullName,
                localName,
                new TypeInfo(((TypeSym) s.lookup(typeName)).type, null, false),
                new CodeLocation());
    }

    public static void addBuiltInSyms(SymTab globalSymTab) {
        for (BuiltInT t : BuiltInT.values()) {
            String typeName = Utils.BuiltinType2ID(t);
            TypeSym s = new TypeSym(typeName, new BuiltinType(typeName));
            globalSymTab.add(typeName, s);
        }
        /* msg:
        *   sender - address
        *   value - uint
        * */
        ArrayList<VarInfo> members = new ArrayList<>();
        VarInfo sender = createVarInfo("msg.sender", "sender", "address", globalSymTab);
        members.add(sender);
        VarInfo value = createVarInfo("msg.value", "value", "uint", globalSymTab);
        members.add(value);
        StructType msgT = new StructType("msgT", members);
        VarInfo msg = new VarInfo("msg", "msg",
                new TypeInfo(msgT, null, false),
                new CodeLocation());
        globalSymTab.add("msg", new VarSym("msg", msg));

        /* send(address, value) */
        members = new ArrayList<>();
        VarInfo recipient = createVarInfo("send.recipient", "recipient", "address", globalSymTab);
        value = createVarInfo("send.value", "value", "uint", globalSymTab);
        members.add(recipient);
        members.add(value);
        FuncInfo sendFuncInfo = new FuncInfo("send", null, members, getBuiltinTypeInfo("bool", globalSymTab), new CodeLocation());
        FuncSym send = new FuncSym("send", sendFuncInfo);
        globalSymTab.add("send", send);
    }

    public static boolean isBuiltinFunc(String funcName) {
        if (funcName.equals("send"))
            return true;
        return false;
    }

    public static String transBuiltinFunc(String funcName, Call call) {
        if (funcName.equals("send")) {
            String recipient = call.args.get(0).toSolCode();
            String value = call.args.get(1).toSolCode();
            return recipient + ".send(" + value + ");";
        }
        else
            return "unknown built-in function";
    }
}


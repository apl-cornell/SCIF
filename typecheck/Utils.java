package typecheck;

import ast.*;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import sherrlocUtils.Constraint;
import sherrlocUtils.Inequality;

import java.io.*;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;

import modules.sherrloc.GenErrorDiagnostic.src.sherrloc.diagnostic.*;

public class Utils {
    public static final String[] BUILTIN_TYPE_NAMES =
            new String[] {"bool", "address", "bytes", "string", "void", "uint"};
            //new String[] {"bool", "int128", "uint256", "address", "bytes", "string", "int", "void", "uint"};
    public static final HashSet<String> BUILTIN_TYPES = new HashSet<>(Arrays.asList(BUILTIN_TYPE_NAMES));

    //public static final String ENDORCE_FUNC_NAME = "endorce";
    public static final String LABEL_TOP = "TOP";
    public static final String LABEL_BOTTOM = "BOT";
    public static final String LABEL_THIS = "this";
    public static final String DEAD = "---DEAD---";
    public static final String KEY = "KEY";
    public static final String SHERRLOC_TOP = LABEL_TOP;
    public static final String SHERRLOC_BOTTOM = LABEL_BOTTOM;

    public static final String SHERRLOC_PASS_INDICATOR = "No errors";
    public static final String SHERRLOC_ERROR_INDICATOR = "wrong";
    public static final String TYPECHECK_PASS_MSG = "The program typechecks.";
    public static final String TYPECHECK_ERROR_MSG = "The program doesn't typecheck.";
    public static final String TYPECHECK_NORESULT_MSG = "No result from ShErrLoc.";


    public static final String ADDRESSTYPE = "address";
    public static final String PUBLIC_DECORATOR = "public";
    public static final String PAYABLE_DECORATOR = "payable";

    public static final String TRUSTCENTER_NAME = "trustCenter";
    public static final String SET_CONTRACT_NAME = "Set";
    public static final String PATH_TO_BASECONTRACTCENTRALIZED = "BaseContractCentralized";

    public static final String ERROR_MESSAGE_LOCK_IN_NONLAST_OPERATION = "Static reentrancy locks should be maintained except during the last operation";
    public static final String ERROR_MESSAGE_LOCK_IN_LAST_OPERATION = "The operation at tail position should respect the final reentrancy lock label";


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
    public static String getLabelNameLock(CodeLocation location) {
        if (location == null) {
            return "LK";
        } else {
            return location.toString() + ".." + "LK";
        }
        /*if (prefix.equals("")) {
            return "LK";
        } else {
            return prefix + ".." + "LK";
        }*/
    }
    public static String getLabelNameInLock(CodeLocation location) {
        if (location == null) {
            return "ILK";
        } else {
            return location.toString() + ".." + "ILK";
        }
    }
    public static String getLabelNameFuncCallPcBefore(String funcName) {
        return funcName + ".." + "call.pc.bfr";
    }
    public static String getLabelNameFuncCallPcAfter(String funcName) {
        return funcName + ".." + "call.pc.aft";
    }
    public static String getLabelNameFuncCallLock(String funcName) {
        return funcName + ".." + "call.lk";
    }
    public static String getLabelNameFuncCallGamma(String funcName) {
        return funcName + ".." + "gamma.lk";
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
    public static String getLabelNameArgLabel(String funcName, VarSym arg) {
        return funcName + "." + arg.name + "..lbl";
    }
    public static String[] runSherrloc(String path, String consFilePath) throws Exception {
        logger.debug("runSherrloc()...");
        //sherrloc.diagnostic.ErrorDiagnosis diagnosis = new sherrloc.diagnostic.ErrorDiagnosis();
        String[] command = new String[] {"bash", "-c", path + "/sherrloc/sherrloc -c " + consFilePath};
        ProcessBuilder pb = new ProcessBuilder(command);
        //pb.inheritIO();
        Process p = pb.start();
        BufferedReader br = new BufferedReader(new InputStreamReader(p.getInputStream()));
        ArrayList<String> list = new ArrayList<>();
        String tmp;
        while ((tmp = br.readLine()) != null) {
            list.add(tmp);
            //System.err.println(tmp);
        }
        p.waitFor();
        logger.debug("finished run SLC, collecting output...");
        p.destroy();
        br.close();
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
        else if (type == BuiltInT.BYTES)
            return "bytes";
        else
            return "unknownT";
    }

    public static void writeCons2File(HashSet<String> constructors, ArrayList<Constraint> assumptions, ArrayList<Constraint> constraints, File outputFile, boolean isIFC) {
        try {
            // transform every "this" to "contractName.this"
            BufferedWriter consFile = new BufferedWriter(new FileWriter(outputFile));
            logger.debug("Writing the constraints of size {}", constraints.size());
            //System.err.println("Writing the constraints of size " + env.cons.size());
            if (!constructors.contains("BOT") && isIFC) {
                constructors.add("BOT");
            }
            if (!constructors.contains("TOP") && isIFC)
                constructors.add("TOP");
            if (!constructors.contains("this") && isIFC)
                constructors.add("this");

            if (!constructors.isEmpty()) {
                for (String principal : constructors) {
                    consFile.write("CONSTRUCTOR " + principal + " 0\n");
                }
            }
            if (!assumptions.isEmpty() || isIFC) {
                consFile.write("%%\n");
                if (isIFC) {
                    for (String x : constructors) {
                        if (!x.equals("BOT") && !x.equals("TOP")) {
                            consFile.write("BOT" + " >= " + x + ";" + "\n");
                        }
                        if (!x.equals("TOP")) {
                            consFile.write("TOP" + " <= " + x + ";" + "\n");
                        }
                    }
                }
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

    public static FuncSym getCurrentFuncInfo(NTCEnv env, ScopeContext now) {
        while (!(now.cur instanceof FunctionDef)) {
            now = now.parent;
        }
        FunctionDef funcNode = (FunctionDef) now.cur;
        Sym sym = env.getCurSym(funcNode.name);
        return ((FuncSym) sym);
    }

    public static TypeSym getBuiltinTypeInfo(String typeName, SymTab s) {
        return (TypeSym) s.lookup(typeName);
    }

    public static VarSym createBuiltInVarInfo(String localName, String typeName, ScopeContext context, SymTab s) {
        return new VarSym(
                localName,
                ((TypeSym) s.lookup(typeName)), new PrimitiveIfLabel(new Name("this")), null, context, false, false);
    }

    public static void addBuiltInSyms(SymTab globalSymTab, TrustSetting trustSetting) {
        for (BuiltInT t : BuiltInT.values()) {
            String typeName = Utils.BuiltinType2ID(t);
            TypeSym s = new BuiltinTypeSym(typeName);
            globalSymTab.add(typeName, s);
        }
        ArrayList<VarSym> members = new ArrayList<>();


        /*
            add address type as a contract type
         */

        /*String contractName = "address";
        ArrayList<TrustConstraint> trustCons = new ArrayList<>();
        SymTab symTab = new SymTab();


        ContractSym contractSym = new ContractSym(contractName, symTab, trustCons);*/

        /* msg:
        *   sender - address
        *   value - uint
        * */
        members = new ArrayList<>();
        ScopeContext emptyContext = new ScopeContext("");
        ScopeContext universalContext = new ScopeContext("UNIVERSAL");
        VarSym sender = createBuiltInVarInfo("sender", "address", emptyContext, globalSymTab);
        members.add(sender);
        VarSym value = createBuiltInVarInfo("value", "uint", emptyContext, globalSymTab);
        members.add(value);
        StructTypeSym msgT = new StructTypeSym("msgT", members);
        VarSym msg = new VarSym("msg",
                msgT, null,
                null, universalContext, false, false);
        globalSymTab.add("msg", new VarSym(msg));

        /* send(address, value) */

        members = new ArrayList<>();
        VarSym recipient = createBuiltInVarInfo("recipient", "address", emptyContext, globalSymTab);
        value = createBuiltInVarInfo("value", "uint", emptyContext, globalSymTab);
        members.add(recipient);
        members.add(value);
        IfLabel thisLabel = new PrimitiveIfLabel(new Name("this"));
        IfLabel botLabel  = new PrimitiveIfLabel(new Name("BOT"));
        FuncLabels funcLabels = new FuncLabels(thisLabel, thisLabel, botLabel);
        FuncSym sendFuncSym = new FuncSym("send", funcLabels, members, getBuiltinTypeInfo("bool", globalSymTab), thisLabel,  new ScopeContext("send"), null);
        globalSymTab.add("send", sendFuncSym);

        /* trustedSend(address, value) */
        members = new ArrayList<>();
        recipient = createBuiltInVarInfo("recipient", "address", emptyContext, globalSymTab);
        value = createBuiltInVarInfo("value", "uint", emptyContext, globalSymTab);
        members.add(recipient);
        members.add(value);
        IfLabel trustedSendLabel  = new PrimitiveIfLabel(new Name("trustedSend"));
        funcLabels = new FuncLabels(trustedSendLabel, trustedSendLabel, trustedSendLabel);
        FuncSym trustedSendFuncSym = new FuncSym("trustedSend", funcLabels, members, getBuiltinTypeInfo("bool", globalSymTab), thisLabel,  new ScopeContext("trustedSend"), null);
        globalSymTab.add("trustedSend", trustedSendFuncSym);

        /* built-in for dynamic options */
        // TODO: change to importing style
        //if (trustSetting != null) {
            //if (trustSetting.dynamicSystemOption == DynamicSystemOption.BaseContractCentralized) {
                // setTrust(address trustee)
                members = new ArrayList<>();
                VarSym trustee = createBuiltInVarInfo("trustee", "address", emptyContext, globalSymTab);
                members.add(trustee);
                funcLabels = new FuncLabels(thisLabel, thisLabel, thisLabel);
                FuncSym setTrustSym = new FuncSym("setTrust", funcLabels, members, getBuiltinTypeInfo("bool", globalSymTab), thisLabel, new ScopeContext("setTrust"), null);
                globalSymTab.add("setTrust", setTrustSym);
            //}
        //}
    }

    public static boolean isBuiltinFunc(String funcName) {
        if (funcName.equals("send") || funcName.equals("setTrust"))
            return true;
        return false;
    }

    public static String transBuiltinFunc(String funcName, Call call) {
        if (funcName.equals("send")) {
            String recipient = call.args.get(0).toSolCode();
            String value = call.args.get(1).toSolCode();
            return recipient + ".call{value: " + value + "}(\"\")";
        }
        else if (funcName.equals("setTrust")) {
            String trustee = call.args.get(0).toSolCode();
            return funcName + "(" + trustee + ")";
        }
        else
            return "unknown built-in function";
    }

    public static boolean emptyFile(String outputFileName) {
        File file = new File(outputFileName);
        return file.length() == 0;
    }

    public static boolean arrayExpressionTypeMatch(ArrayList<Expression> x, ArrayList<Expression> y) {

        if (!(x == null && y == null)) {
            if (x == null || y == null || x.size() != y.size())
                return false;
            int index = 0;
            while (index < x.size()) {
                if (!x.get(index).typeMatch(y.get(index)))
                    return false;
                ++index;
            }
        }
        return true;
    }

    public static String getLabelNameContract(String name) {
        return name + "." + "codeLbl";
    }

    public static DynamicSystemOption resolveDynamicOption(String dynamicOption) {
        switch (dynamicOption) {
            case "BaseContractCentralized" :
                return DynamicSystemOption.BaseContractCentralized;
            case "Decentralized" :
                return DynamicSystemOption.Decentralized;
        }
        return null; //TODO error report
    }

    public static String translateSLCSuggestion(HashMap<String, Program> programMap, String s, boolean DEBUG) {
        if (s.charAt(0) != '-') return null;
        if (DEBUG) System.out.println(s);
        //if (true) return s;
        int l = s.indexOf('['), r = s.indexOf(']');
        if (l == -1 || s.charAt(l + 1) != '\"') return  null;
        ++l;
        String explanation = "";
        while (s.charAt(l + 1) != '\"') {
            ++l;
            explanation += s.charAt(l);
        }
        l += 2;

        if (!Character.isDigit(s.charAt(l + 1)))
            return null;
        String slin = "", scol = "";
        while (s.charAt(l + 1) != ',') {
            ++l;
            slin = slin + s.charAt(l);
        }
        ++l;
        while (s.charAt(l + 1) != '-') {
            ++l;
            scol = scol + s.charAt(l);
        }
        int lin = Integer.parseInt(slin), col = Integer.parseInt(scol);

        int p = explanation.indexOf('@');
        String contractName = explanation.substring(p + 1);
        explanation = explanation.substring(0, p);
        //System.out.println("position of @:" + p + " " + contractName);
        Program program = programMap.get(contractName);

        String rtn = program.getProgramName() + "(" + slin + "," + scol + "): " + explanation + ".\n";
        rtn += program.getSourceCodeLine(lin - 1) + "\n";
        for (int i = 1; i < col; ++i)
            rtn += " ";
        rtn += '^';

        return rtn;
    }

    public static String ordNumString(int i) {
        if (i == 1)
            return "1st";
        else if (i == 2)
            return "2nd";
        else if (i == 3)
            return "3rd";
        else
            return i + "th";
    }

    public static String makeJoin(String lhs, String rhs) {
        return "(" + lhs + " ⊔ " + rhs + ")";
    }
}


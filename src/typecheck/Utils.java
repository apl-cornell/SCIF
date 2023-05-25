package typecheck;

import ast.*;
import java.net.URL;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import sherrloc.constraint.ast.Constructor;
import sherrloc.constraint.ast.Hypothesis;
import sherrloc.diagnostic.DiagnosticOptions;
import sherrloc.diagnostic.ErrorDiagnosis;
import sherrloc.graph.Variance;
import typecheck.sherrlocUtils.Constraint;
import typecheck.sherrlocUtils.Inequality;

import java.io.*;
import java.util.*;


public class Utils {

    public static final String[] BUILTIN_TYPE_NAMES =
            new String[]{"bool", "address", "bytes", "string", "void", "uint", "principal"};
    //new String[] {"bool", "int128", "uint256", "address", "bytes", "string", "int", "void", "uint"};
    public static final HashSet<String> BUILTIN_TYPES = new HashSet<>(
            Arrays.asList(BUILTIN_TYPE_NAMES));

    //public static final String ENDORCE_FUNC_NAME = "endorce";
    public static final String LABEL_TOP = "TOP";
    public static final String LABEL_BOTTOM = "any";
    public static final String LABEL_THIS = "this";
    public static final String LABEL_SENDER = "sender";
    public static final String DEAD = "---DEAD---";
    public static final String KEY = "KEY";
    public static final String SHERRLOC_TOP = LABEL_TOP;
    public static final String SHERRLOC_BOTTOM = LABEL_BOTTOM;

    public static final String SHERRLOC_PASS_INDICATOR = "No errors";
    public static final String SHERRLOC_ERROR_INDICATOR = "wrong";
    public static final String TYPECHECK_PASS_MSG = "The program type-checks.";
    public static final String TYPECHECK_ERROR_MSG = "The program doesn't type-check.";
    public static final String TYPECHECK_NORESULT_MSG = "No result from ShErrLoc.";


    public static final String ADDRESS_TYPE = "address";
    public static final String MAP_TYPE = "map";
    public static final String PUBLIC_DECORATOR = "public";
    public static final String PROTECTED_DECORATOR = "protected";
    public static final String FINAL_DECORATOR = "final";
    public static final String PAYABLE_DECORATOR = "payable";

    public static final String TRUSTCENTER_NAME = "trustCenter";
    public static final String SET_CONTRACT_NAME = "Set";
    public static final String PATH_TO_BASECONTRACTCENTRALIZED = "BaseContractCentralized";

    public static final String ERROR_MESSAGE_LOCK_IN_NONLAST_OPERATION = "Static reentrancy locks should be maintained except during the last operation";
    public static final String ERROR_MESSAGE_LOCK_IN_LAST_OPERATION = "The operation at tail position should respect the final reentrancy lock label";

    public static final String DEBUG_UNKNOWN_CONTRACT_NAME = "UNKNOWN";
    public static final String ANONYMOUS_VARIABLE_NAME = "ANONYMOUS";
    public static final String PRINCIPAL_TYPE = "principal";
    public static final String EXCEPTION_ERROR_NAME = "error";
    public static final String METHOD_SEND_NAME = "send";
    public static final String METHOD_BALANCE_NAME = "balance";
    public static final CodeLocation BUILTIN_LOCATION = new CodeLocation(0, 0, "BUILTIN");
    public static final String LABEL_PAYVALUE = "value";
    public static final String VOID_TYPE = "void";
    public static final String BUILTIN_CONTRACT = "Builtin";
    public static final String BASECONTRACTNAME = "BaseContract";
    public static final Iterable<? extends File> BUILTIN_FILES = generateBuiltInFiles();
    public static final List<String> BUILTIN_FILENAMES = Arrays.asList(
            "builtin_files/BaseContract.scif",
            "builtin_files/ILockManager.scif",
            "builtin_files/ITrustManager.scif"
    );

    private static Iterable<? extends File> generateBuiltInFiles() {
        List<File> builtin_files = new ArrayList<>();
        assert BUILTIN_FILENAMES != null;
        for (String filename : BUILTIN_FILENAMES) {
            URL input = ClassLoader.getSystemResource(filename);
            File inputFile = new File((input.getFile()));
            builtin_files.add(inputFile);
        }
        return builtin_files;
    }

    public static boolean isPrimitiveType(String x) {
        return BUILTIN_TYPES.contains(x);
    }

    /*public static IfConstraint genCons(String from, String to, CodeLocation location) {
        // right flows to left
        return new IfConstraint("<=", from, to, location);
    }*/
    /*public static IfConstraint genNewlineCons() {
        return new IfConstraint();
    }*/

    public static String getLabelNamePc(String location) {
        if (location == null) {
            return "PC";
        } else {
            return location + "." + "PC";
        }
    }

    public static String getLabelNameLock(String location) {
        if (location == null) {
            return "LK";
        } else {
            return location + "." + "LK";
        }
        /*if (prefix.equals("")) {
            return "LK";
        } else {
            return prefix + ".." + "LK";
        }*/
    }

    public static String getLabelNameInLock(String funcFullName) {
        if (funcFullName == null) {
            return "ILK";
        } else {
            return funcFullName + "." + "ILK";
        }
    }

    /*public static String getLabelNameFuncCallPcBefore(String funcName) {
        return funcName + "." + "call.pc.bfr";
    }*/

//    public static String getLabelNameFuncCallPcAfter(String funcName) {
//        return funcName + "." + "call.pc.aft";
//    }

//    public static String getLabelNameCallPcEnd(String funcName) {
//        return funcName + "." + "call.pc.end";
//    }

    public static String getLabelNameFuncCallLock(String funcName) {
        return funcName + "." + "call.lk";
    }

//    public static String getLabelNameFuncCallGamma(String funcName) {
//        return funcName + "." + "gamma.lk";
//    }

    /*public static String getLabelNameFuncCallBefore(String funcName) {
        return funcName + ".." + "call.before";
    }
    public static String getLabelNameFuncCallAfter(String funcName) {
        return funcName + ".." + "call.after";
    }*/
    public static String getLabelNameFuncRtnValue(String funcName) {
        return funcName + "." + "rtn.v";
    }

    public static String getLabelNameFuncRtnLock(String funcName) {
        return funcName + "." + "rtn.lk";
    }

    public static String getLabelNameFuncRtnPc(String funcName) {
        return funcName + "." + "rtn.pc";
    }

    public static String getLabelNameArgLabel(String funcName, VarSym arg) {
        return funcName + "." + arg.getName() + ".lbl";
    }

    public static String getLabelNameFuncExpLabel(String funcName, String name) {
        return funcName + "." + name + ".lbl";
    }

    public static sherrloc.diagnostic.DiagnosticConstraintResult runSherrloc(String consFilePath)
            throws Exception {
        logger.debug("runSherrloc()...");
        String[] args = new String[]{"-c", consFilePath};
        DiagnosticOptions options = new DiagnosticOptions(args);
        ErrorDiagnosis ana = ErrorDiagnosis.getAnalysisInstance(options);

        sherrloc.diagnostic.DiagnosticConstraintResult result = ana.getConstraintResult();
        return result;

        //sherrloc.diagnostic.ErrorDiagnosis diagnosis = new sherrloc.diagnostic.ErrorDiagnosis();
        /*String[] command = new String[] {"bash", "-c", path + "/sherrloc/sherrloc -c " + consFilePath};
        ProcessBuilder pb = new ProcessBuilder(command);
        pb.inheritIO();
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
        return list.toArray(new String[0]);*/
    }

    public static String joinLabels(String lhs, String rhs) {
        return "(" + lhs + " ⊔ " + rhs + ")";
    }

    public static String meetLabels(String lhs, String rhs) {
        return "(" + lhs + " ⊓ " + rhs + ")";
    }

    public static String BuiltinType2ID(BuiltInT type) {
        if (type == BuiltInT.UINT) {
            return "uint";
        } else if (type == BuiltInT.BOOL) {
            return "bool";
        } else if (type == BuiltInT.STRING) {
            return "string";
        } else if (type == BuiltInT.VOID) {
            return "void";
        } else if (type == BuiltInT.ADDRESS) {
            return "address";
        } else if (type == BuiltInT.BYTES) {
            return "bytes";
        } else if (type == BuiltInT.PRINCIPAL) {
            return "principal";
        } else {
            return "unknownT";
        }
    }

    public static boolean writeCons2File(Set<? extends Sym> constructors, List<Constraint> assumptions,
            List<Constraint> constraints, File outputFile, boolean isIFC) {
        try {
            // transform every "this" to "contractName.this"
            BufferedWriter consFile = new BufferedWriter(new FileWriter(outputFile));
            if (constraints.size() == 0) {
                return false;
            }
            logger.debug("Writing the constraints of size {}", constraints.size());
            //System.err.println("Writing the constraints of size " + env.cons.size());
//            VarSym bot = new VarSym();
//            if (!constructors.contains("BOT") && isIFC) {
//                constructors.add("BOT");
//            }
//            if (!constructors.contains("TOP") && isIFC) {
//                constructors.add("TOP");
//            }
            /*if (!constructors.contains("this") && isIFC) {
                constructors.add("this");
            }*/

            Sym bot = null, top = null;
            if (!constructors.isEmpty()) {
                for (Sym principal : constructors) {
                    if (principal.getName().equals(LABEL_BOTTOM)) {
                        bot = principal;
                    }
                    if (principal.getName().equals(LABEL_TOP)) {
                        top = principal;
                    }
                    consFile.write("CONSTRUCTOR " + principal.toSHErrLocFmt() + " 0\n");
                }
            }
            if (!assumptions.isEmpty() || isIFC) {
                consFile.write("%%\n");
                assert bot != null;
                assert top != null;
                if (isIFC) {
                    for (Sym x : constructors) {
                        if (!x.equals(bot) && !x.equals(top)) {
                            consFile.write(bot.toSHErrLocFmt() + " >= " + x.toSHErrLocFmt() + ";" + "\n");
                        }
                        if (!x.equals(top)) {
                            consFile.write(top.toSHErrLocFmt() + " <= " + x.toSHErrLocFmt() + ";" + "\n");
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
            return false;
        }
        return true;
    }

    public static boolean SLCinput(HashSet<String> constructors, ArrayList<Constraint> assumptions,
            ArrayList<Constraint> constraints, boolean isIFC) {
        try {
            sherrloc.constraint.ast.Hypothesis hypothesis = new Hypothesis();
            ArrayList<sherrloc.constraint.ast.Axiom> axioms = new ArrayList<>();
            Set<sherrloc.constraint.ast.Constraint> constraintSet = new HashSet<>();
            // transform every "this" to "contractName.this"
            //BufferedWriter consFile = new BufferedWriter(new FileWriter(outputFile));
            logger.debug("Writing the constraints of size {}", constraints.size());
            if (constraints.size() == 0) {
                return false;
            }
            //System.err.println("Writing the constraints of size " + env.cons.size());
            if (!constructors.contains(LABEL_BOTTOM) && isIFC) {
                constructors.add(LABEL_BOTTOM);
            }
            if (!constructors.contains(LABEL_TOP) && isIFC) {
                constructors.add(LABEL_TOP);
            }
            if (!constructors.contains(LABEL_THIS) && isIFC) {
                constructors.add(LABEL_THIS);
            }

            if (!constructors.isEmpty()) {
                for (String principal : constructors) {
                    sherrloc.constraint.ast.Constructor constructor = new Constructor(principal, 0,
                            0, Variance.POS, sherrloc.constraint.ast.Position.EmptyPosition());
                    // consFile.write("CONSTRUCTOR " + principal + " 0\n");
                }
            }
            if (!assumptions.isEmpty() || isIFC) {
                //consFile.write("%%\n");
                if (isIFC) {
                    for (String x : constructors) {
                        if (!x.equals(LABEL_BOTTOM) && !x.equals(LABEL_TOP)) {
                            //          consFile.write("BOT" + " >= " + x + ";" + "\n");
                        }
                        if (!x.equals(LABEL_TOP)) {
                            //          consFile.write("TOP" + " <= " + x + ";" + "\n");
                        }
                    }
                }
                for (Constraint con : assumptions) {
                    //  consFile.write(con.toSHErrLocFmt(false) + "\n");
                }
                //consFile.write("%%\n");
            } else {
                // consFile.write("\n");
            }
            if (!constraints.isEmpty()) {
                for (Constraint con : constraints) {
                    //consFile.write(con.toSHErrLocFmt(true) + "\n");
                }
            }
            //consFile.close();
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
        return true;
    }

    protected static final Logger logger = LogManager.getLogger();

    public static FuncSym getCurrentFuncInfo(NTCEnv env, ScopeContext now) {
        while (!(now.cur() instanceof FunctionDef)) {
            now = now.parent();
        }
        FunctionDef funcNode = (FunctionDef) now.cur();
        Sym sym = env.getCurSym(funcNode.getName());
        return ((FuncSym) sym);
    }

    public static TypeSym getBuiltinTypeInfo(String typeName, SymTab s) {
        return (TypeSym) s.lookup(typeName);
    }

    /*private static ExceptionTypeSym builtin_error_sym() {
        return new ExceptionTypeSym("error", new PrimitiveIfLabel(new Name(LABEL_BOTTOM)), new ArrayList<>());
    }

     */

  /*  public static boolean isBuiltinFunc(String funcName) {
        if (funcName.equals("send") || funcName.equals("setTrust")) {
            return true;
        }
        return false;
    }

   */

 /*   public static String transBuiltinFunc(String funcName, Call call) {
        if (funcName.equals("send")) {
            String recipient = call.getArgAt(0).toSolCode();
            String value = call.getArgAt(1).toSolCode();
            return recipient + ".call{value: " + value + "}(\"\")";
        } else if (funcName.equals("setTrust")) {
            String trustee = call.getArgAt(0).toSolCode();
            return funcName + "(" + trustee + ")";
        } else {
            return "unknown built-in function";
        }
    }

  */

    public static boolean emptyFile(String outputFileName) {
        File file = new File(outputFileName);
        return file.length() == 0;
    }

    public static boolean arrayExpressionTypeMatch(ArrayList<Expression> x,
            ArrayList<Expression> y) {

        if (!(x == null && y == null)) {
            if (x == null || y == null || x.size() != y.size()) {
                return false;
            }
            int index = 0;
            while (index < x.size()) {
                if (!x.get(index).typeMatch(y.get(index))) {
                    return false;
                }
                ++index;
            }
        }
        return true;
    }

    public static String getLabelNameContract(ScopeContext context) {
        return context.getSHErrLocName() + "." + "codeLbl";
    }

    public static DynamicSystemOption resolveDynamicOption(String dynamicOption) {
        if (dynamicOption == null) {
            return DynamicSystemOption.BaseContractCentralized;
        }
        return switch (dynamicOption) {
            case "BaseContractCentralized" -> DynamicSystemOption.BaseContractCentralized;
            case "Decentralized" -> DynamicSystemOption.Decentralized;
            default -> null;
        };
        //TODO error report
    }

    public static String translateSLCSuggestion(HashMap<String, SourceFile> programMap, String s,
            boolean DEBUG) {
        if (s.charAt(0) != '-') {
            return null;
        }
        if (DEBUG) {
            System.err.println(s);
        }

        //if (true) return s;
        int l = s.indexOf('['), r = s.indexOf(']');
        if (l == -1 || s.charAt(l + 1) != '\"') {
            return null;
        }
        ++l;
        String explanation = "";
        while (s.charAt(l + 1) != '\"') {
            ++l;
            explanation += s.charAt(l);
        }
        l += 2;

        if (!Character.isDigit(s.charAt(l + 1))) {
            return null;
        }
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
        SourceFile program = programMap.get(contractName);

        String rtn =
                program.getSourceFileId() + "(" + slin + "," + scol + "): " + explanation + ".\n";
        rtn += program.getSourceCodeLine(lin - 1) + "\n";
        for (int i = 1; i < col; ++i) {
            rtn += " ";
        }
        rtn += '^';

        return rtn;
    }

    public static String SLCSuggestionToString(HashMap<String, SourceFile> programMap,
            sherrloc.diagnostic.explanation.Explanation exp, boolean DEBUG) {
        String s = exp.toConsoleStringWithExp();
        if (DEBUG) {
            System.err.println(s + "#" + exp.getWeight());
        }

        //if (true) return s;
        int l = s.indexOf('['), r = s.indexOf(']');
        if (l == -1 || s.charAt(l + 1) != '\"') {
            // if (DEBUG) System.out.println("no explanation found");
            return null;
        }
        ++l;
        String explanation = "";
        while (s.charAt(l + 1) != '\"') {
            ++l;
            explanation += s.charAt(l);
        }
        l += 2;

        // if (DEBUG) System.out.println(explanation);
        if (!Character.isDigit(s.charAt(l + 1))) {
            // if (DEBUG) System.out.println("no range digit found");
            return null;
        }
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
        // if (DEBUG) System.out.println("position of #:" + p + " " + contractName);
        SourceFile program = programMap.get(contractName);

        if (lin == 0 || col == 0) {
            // Built-in or default
            return program.getSourceFileFullName() + "\n" + explanation + ".\n";
        }
        String rtn =
                program.getSourceFileFullName() + "(" + slin + "," + scol + "): " + "\n" + explanation + ".\n";
        rtn += program.getSourceCodeLine(lin - 1) + "\n";
        for (int i = 1; i < col; ++i) {
            rtn += " ";
        }
        rtn += '^';

        return rtn;
    }

    public static String ordNumString(int i) {
        if (i == 1) {
            return "1st";
        } else if (i == 2) {
            return "2nd";
        } else if (i == 3) {
            return "3rd";
        } else {
            return i + "th";
        }
    }

    public static void contextFlow(VisitEnv env, Context outContext, Context funcEndContext,
            CodeLocation location) {
        env.addTrustConstraint(new Constraint(new Inequality(outContext.lambda, funcEndContext.lambda),
                env.hypothesis(), location, env.curContractSym().getName(),
                "actually-maintained lock of the last sub-statement flows to parent-statement's one"));
        env.addTrustConstraint(
                new Constraint(new Inequality(outContext.pc, funcEndContext.pc), env.hypothesis(),
                        location, env.curContractSym().getName(),
                        "normal termination control flow of the last sub-statement flows to parent-statement's one"));
    }

    public static ExceptionTypeSym getNormalPathException() {
        return new ExceptionTypeSym("*n", new ArrayList<>(), globalScopeContext());
    }

    public static PsiUnit joinPsiUnit(PsiUnit u1, PsiUnit u2) {
        return new PsiUnit(joinContext(u1.c, u2.c), u1.catchable && u2.catchable);
    }

    private static Context joinContext(Context c1, Context c2) {
        return new Context(joinLabels(c1.pc, c2.pc), joinLabels(c1.lambda, c2.lambda));
    }

    public static ExceptionTypeSym getReturnPathException() {
        return new ExceptionTypeSym("*r", new ArrayList<>(), globalScopeContext());
    }

    public static void addBuiltInTypes(SymTab symTab) {
        for (BuiltInT t : BuiltInT.values()) {
            String typeName = Utils.BuiltinType2ID(t);
            TypeSym s = new BuiltinTypeSym(typeName);
            symTab.add(typeName, s);
        }
    }

    public static CodeLocation placeholder() {
        return new CodeLocation();
    }

    public static ScopeContext globalScopeContext() {
        return new ScopeContext(null, null);
    }
}


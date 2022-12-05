import java.io.*;

import ast.*;
import java_cup.runtime.*;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import typecheck.sherrlocUtils.Constraint;
import typecheck.sherrlocUtils.Hypothesis;
import typecheck.sherrlocUtils.Inequality;
import typecheck.sherrlocUtils.Relation;
import typecheck.*;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.*;

public class TypeChecker {

    public static void main(String[] args) {
        File inputFile = new File(args[0]);
        File outputFile = new File(args[1]);
        //typecheck(inputFile, outputFile);
    }

    public static List<SourceFile> regularTypecheck(ArrayList<File> inputFiles, File outputFile,
            boolean DEBUG) {

        logger.trace("typecheck starts");

        List<SourceFile> roots = new ArrayList<>();
        for (File inputFile : inputFiles) {
            try {
                logger.debug(inputFile);

                //Lexer lexer = new Lexer(new FileReader(inputFile));
                //Parser p = new Parser(lexer);

                Symbol result = Parser.parse(inputFile, null);//p.parse();
                SourceFile root = (SourceFile) result.value;
                // TODO root.setName(inputFile.getName());
                List<String> sourceCode = Files.readAllLines(Paths.get(inputFile.getAbsolutePath()),
                        StandardCharsets.UTF_8);
                root.setSourceCode(sourceCode);
                roots.add(root);
                logger.debug("Finish");
                //System.err.println("Finish\n");
            } catch (Exception e) {
                e.printStackTrace();
                return null;
            }
        }

        // Step 1: typecheck, generate constraints and check via SHErrLoc

        // Code-paste superclasses' methods and data fields
        HashMap<String, Contract> contractMap = new HashMap<>();
        InheritGraph graph = new InheritGraph();
        for (SourceFile root : roots) {
            if (!root.ntcInherit(graph)) {
                // TODO: doesn't typecheck
                return null;
            }
            if (root.getContract() != null) {
                contractMap.put(root.getContractName(), root.getContract());
            }
        }

        logger.debug(
                " check if there is any non-existent contract name: " + contractMap.keySet() + " "
                        + graph.getAllNodes());
        // check if there is any non-existent contract name
        for (String contractName : graph.getAllNodes()) {
            if (!contractMap.containsKey(contractName)) {
                // TODO: mentioning non-existent contract
                return null;
            }
        }

        logger.debug(" code-paste in a topological order");
        // code-paste in a topological order
        for (String x : graph.getTopologicalQueue()) {
            Node rt = null;
            for (Node root : roots) {
                if (((SourceFile) root).containContract(x)) {
                    rt = root;
                    break;
                }
            }
            if (rt == null) {
                // TODO: contract not found
                return null;
            }
            if (!((SourceFile) rt).codePasteContract(x, contractMap)) {
                // TODO: inherit failed
                return null;
            }
        }

        // Collect global info
        NTCEnv NTCenv = new NTCEnv();
        for (SourceFile root : roots) {
            NTCenv.programMap.put(root.getContractName(), root);
            root.passScopeContext(null);
            if (!root.ntcGlobalInfo(NTCenv, null)) {
                // doesn't typecheck
                return null;
            }
        }

        logger.debug("Current Contracts: " + NTCenv.globalSymTab.getTypeSet());

        // Generate constraints
        for (Node root : roots) {
            root.ntcGenCons(NTCenv, null);
        }
        // Check using SHErrLoc and get a solution

        logger.debug("generating cons file for NTC");
        // constructors: all types
        // assumptions: none or relations between types
        // constraints
        if (!Utils.writeCons2File(NTCenv.getTypeSet(), NTCenv.getTypeRelationCons(), NTCenv.cons,
                outputFile, false)) {
            return roots;
        }
        boolean result = false;
        try {
            result = runSLC(NTCenv.programMap, outputFile.getAbsolutePath(), DEBUG);
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }

        return result == true ? roots : null;
    }

    public static boolean ifcTypecheck(List<SourceFile> roots, List<File> outputFiles,
            boolean DEBUG) {

        HashMap<SourceFile, File> outputFileMap = new HashMap<>();
        for (int i = 0; i < roots.size(); ++i) {
            outputFileMap.put(roots.get(i), outputFiles.get(i));
        }

        // HashMap<String, ContractSym> contractMap = new HashMap<>();
        SymTab contractMap = new SymTab();
        ArrayList<String> contractNames = new ArrayList<>();
        //HashSet<String> principalSet = new HashSet<>();
        //env.principalSet.add("this");

        //ArrayList<Constraint> cons = new ArrayList<>();

        for (SourceFile root : roots) {
            ContractSym contractSym = new ContractSym();
            contractSym.name = ((SourceFile) root).getContractName();
            contractSym.astNode = root;
            contractNames.add(contractSym.name);
            contractMap.add(contractSym.name, contractSym);
            //root.findPrincipal(principalSet);
        }

        logger.debug("contracts: \n" + contractMap.getTypeSet());
        int idx = 0;
        for (SourceFile root : roots) {
            ContractSym contractSym = (ContractSym) contractMap.lookup(contractNames.get(idx));
            ++idx;
            contractSym.symTab = new SymTab(contractMap);
            root.globalInfoVisit(contractSym);
            if (contractNames.contains(contractSym.name)) {
                //TODO: duplicate contract names
            }
            logger.debug(contractSym.toString());
            //root.findPrincipal(principalSet);
        }

        logger.debug("starting to ifc typecheck");

        VisitEnv env = new VisitEnv(
                new Context(),
                new ArrayList<>(),
                new ArrayList<>(),
                contractMap,
                contractMap,
                new Hypothesis(),
                new HashSet<>(),
                null,
                new HashMap<>()
        );

        // env.globalSymTab = env.curSymTab = contractMap;

        // collect constraints generating for signatures for each contract
        // a map from filename -> contract name -> signature info
        /*for (Program root : roots) {
            buildSignatureConstraints(root, env);
        }*/

        for (SourceFile root : roots) {
            env.programMap.put(root.getContractName(), root);
            env.sigReq.clear();
            if (!ifcTypecheck(root, env, outputFileMap.get(root), DEBUG)) {
                return false;
            }
        }

        logger.trace("typecheck finishes");
        return true;
    }

    private static void buildSignatureConstraints(String contractName, VisitEnv env,
            String namespace, String curContractName) {
        List<Constraint> cons = env.cons;
        List<Constraint> trustCons = env.trustCons;
        // String contractName = root.getContractName();//contractNames.get(fileIdx);
        ContractSym contractSym = env.getContract(contractName);
        logger.debug("cururent Contract: " + contractName + "\n" + contractSym + "\n"
                + env.curSymTab.getTypeSet());
        // generate trust relationship dec constraints

        if (namespace != "") {
            trustCons.add(
                    new Constraint(new Inequality(namespace, Relation.EQ, namespace + "..this"),
                            null, curContractName,
                            "TODO"));
        }
        String ifNameContract = contractSym.getLabelNameContract();
        String ifContract = contractSym.getLabelContract();
        if (namespace == "") {
            cons.add(new Constraint(new Inequality(ifNameContract, Relation.EQ, ifContract),
                    contractSym.ifl.getLocation(), contractName,
                    "Integrity label of contract " + ifNameContract + " may be incorrect"));

            for (TrustConstraint trustConstraint : contractSym.trustSetting.getTrust_list()) {
                trustCons.add(new Constraint(
                        new Inequality(trustConstraint.lhs.toSherrlocFmt(contractName),
                                trustConstraint.optor,
                                trustConstraint.rhs.toSherrlocFmt(contractName)),
                        trustConstraint.getLocation(), contractName,
                        "Static trust relationship"));
            }
        }

        env.curContractSym = contractSym;
        env.curSymTab = contractSym.symTab;
        env.curSymTab.setParent(env.globalSymTab);//TODO

        for (HashMap.Entry<String, FuncSym> funcPair : contractSym.symTab.getFuncs().entrySet()) {
            FuncSym func = funcPair.getValue();
            //TODO: simplify
            String ifNameCallBeforeLabel = func.getLabelNameCallPcBefore(namespace);
            String ifNameCallAfterLabel = func.getLabelNameCallPcAfter(namespace);
            // String ifNameCallLockLabel = func.getLabelNameCallLock(namespace);
            String ifNameCallGammaLabel = func.getLabelNameCallGamma(namespace);
            String ifCallBeforeLabel = func.getCallPcLabel(namespace);
            String ifCallAfterLabel = func.getCallAfterLabel(namespace);
            String ifCallLockLabel = func.getCallLockLabel(namespace);
            logger.debug("add func's sig constraints: [" + func.funcName + "]");
            // logger.debug(ifNameCallBeforeLabel + "\n" + ifNameCallAfterLabel + "\n" + ifNameCallLockLabel + "\n" + ifCallAfterLabel + "\n" +ifCallLockLabel);
            if (ifCallBeforeLabel != null) {
                cons.add(new Constraint(
                        new Inequality(ifCallBeforeLabel, Relation.EQ, ifNameCallBeforeLabel),
                        func.funcLabels.begin_pc.getLocation(), contractName,
                        "Integrity requirement to call this method may be incorrect"));
            }
            if (ifCallAfterLabel != null) {
                cons.add(new Constraint(
                        new Inequality(ifCallAfterLabel, Relation.EQ, ifNameCallAfterLabel),
                        func.funcLabels.to_pc.getLocation(), contractName,
                        "Integrity pc level autoendorsed to when calling this method may be incorrect"));
            }

            if (ifCallLockLabel != null) {
                cons.add(new Constraint(
                        new Inequality(ifCallLockLabel, Relation.EQ, ifNameCallGammaLabel),
                        func.funcLabels.gamma_label.getLocation(), contractName,
                        "The final reentrancy lock label may be declared incorrectly"));

            }

            String ifNameReturnLabel = func.getLabelNameRtnValue();
            String ifReturnLabel = func.getRtnValueLabel();
            if (ifReturnLabel != null) {
                cons.add(new Constraint(
                        new Inequality(ifReturnLabel, Relation.EQ, ifNameReturnLabel),
                        func.location, contractName,
                        "Integrity label of this method's return value may be incorrect"));
            }

            for (int i = 0; i < func.parameters.size(); ++i) {
                VarSym arg = func.parameters.get(i);
                String ifNameArgLabel = func.getLabelNameArg(namespace, i);
                String ifArgLabel = arg.getLabel(namespace);
                if (ifArgLabel != null) {
                    cons.add(new Constraint(new Inequality(ifNameArgLabel, Relation.EQ, ifArgLabel),
                            arg.ifl.getLocation(), contractName,
                            "Argument " + arg.name + " may be labeled incorrectly"));
                    trustCons.add(
                            new Constraint(new Inequality(ifNameCallBeforeLabel, ifNameArgLabel),
                                    arg.ifl.getLocation(), contractName,
                                    "Argument " + arg.name
                                            + " must be no more trusted than caller's integrity"));
                }
            }

        }
        cons.add(new Constraint());
        // env.addSigCons(contractName, trustCons, cons);
    }

    private static boolean ifcTypecheck(SourceFile root, VisitEnv env, File outputFile,
            boolean DEBUG) {
        String contractName = root.getContractName();//contractNames.get(fileIdx);
        ContractSym contractSym = env.getContract(contractName);
        logger.debug("cururent Contract: " + contractName + "\n" + contractSym + "\n"
                + env.curSymTab.getTypeSet());

        env.curContractSym = contractSym;
        env.curSymTab = contractSym.symTab;
        env.curSymTab.setParent(env.globalSymTab);//TODO
        env.cons = new ArrayList<>();

        logger.debug("Display varMap:");
        //System.err.println("Display varMap:\n");
        for (HashMap.Entry<String, VarSym> varPair : contractSym.symTab.getVars().entrySet()) {
            VarSym var = varPair.getValue();
            String varName = var.labelToSherrlocFmt();
            String ifLabel = var.getLabel();
            if (ifLabel != null) {
                env.cons.add(
                        new Constraint(new Inequality(varName, Relation.EQ, ifLabel), var.location,
                                contractName,
                                "Variable " + var.name + " may be labeled incorrectly"));

                //env.cons.add(new Constraint(new Inequality(if Label, varName), var.location));

            }
            logger.debug(varName);
            logger.debug(": {}", var);
            //System.err.println(varName);
            //System.err.println(": " + var + "\n");
        }

        //SigCons curSigCons = env.getSigCons(contractName);
        //env.trustCons.addAll(curSigCons.trustcons);
        //env.cons.addAll(curSigCons.cons);

        Node tmp = root;
        if (!(tmp instanceof SourceFile)) {
            // TODO: not supported currently
            Interface anInterface = (Interface) tmp;
            for (FunctionSig functionSig : anInterface.funcSigs) {
                functionSig.findPrincipal(env.principalSet);
            }

        } else {
            SourceFile sourceFile = (SourceFile) tmp;
            // env.varNameMap = new LookupMaps(varMap);

            sourceFile.genConsVisit(env, true);
            buildSignatureConstraints(sourceFile.getContractName(), env, "",
                    sourceFile.getContractName());
            env.sigReq.forEach((name, curContractName) -> {
                buildSignatureConstraints(curContractName, env, name, sourceFile.getContractName());
            });
        }

        if (!Utils.writeCons2File(env.principalSet, env.trustCons, env.cons, outputFile, true)) {
            return true;
        }
        boolean result = false;
        try {
            result = runSLC(env.programMap, outputFile.getAbsolutePath(), DEBUG);
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
        return result;
    }

    static boolean runSLC(HashMap<String, SourceFile> programMap, String outputFileName,
            boolean DEBUG) throws Exception {
        logger.trace("running SLC");


        String classDirectoryPath = new File(
                SCIF.class.getProtectionDomain().getCodeSource().getLocation().toURI()).getPath();
        sherrloc.diagnostic.DiagnosticConstraintResult result = Utils.runSherrloc(outputFileName);
        logger.debug("runSLC: " + result);
        if (result.success()) {
            System.out.println(Utils.TYPECHECK_PASS_MSG);
        } else {
            System.out.println(Utils.TYPECHECK_ERROR_MSG);
            double best = Double.MAX_VALUE;
            boolean seced = false;
            System.out.println("Places most likely to be wrong:");
            if (DEBUG) {
                System.out.println("No of places: " + result.getSuggestions().size());
            }
            int idx = 1;
            for (int i = 0; i < result.getSuggestions().size(); ++i) {
                double weight = result.getSuggestions().get(i).getWeight();
                if (best > weight) {
                    best = weight;
                }
                if (!seced && best < weight) {
                    seced = true;
                    System.out.println("Some other possible places:");
                }
                String s = Utils.SLCSuggestionToString(programMap, result.getSuggestions().get(i),
                        DEBUG);
                if (s != null) {
                    System.out.println(idx + ":");
                    System.out.println(s);
                    idx += 1;
                }
            }
            return false;
        }
        return true;
    }

    protected static final Logger logger = LogManager.getLogger();
}

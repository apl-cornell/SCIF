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

    /*
        Given a list of SCIF source files, this method typechecks all code ignoring information flow control.
        It generates constraints in SHErrLoc format and put them in outputFile, then runs ShErrLoc to get error info.
     */
    public static List<SourceFile> regularTypecheck(ArrayList<File> inputFiles, File outputFile,
            boolean DEBUG) {

        logger.trace("typecheck starts...");

        /*
            parse all SCIF source files and store AST roots in roots.
         */
        List<SourceFile> roots = new ArrayList<>();
        for (File inputFile : inputFiles) {
            try {
                logger.debug(inputFile);

                //Lexer lexer = new Lexer(new FileReader(inputFile));
                //Parser p = new Parser(lexer);

                Symbol result = Parser.parse(inputFile, null);//p.parse();
                SourceFile root = (SourceFile) result.value;
                // TODO root.setName(inputFile.name());
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
            SourceFile rt = null;
            for (SourceFile root : roots) {
                if (root.containContract(x)) {
                    rt = root;
                    break;
                }
            }
            if (rt == null) {
                // TODO: contract not found
                assert false;
                return null;
            }
            if (!rt.codePasteContract(x, contractMap)) {
                // TODO: inherit failed
                assert false;
                return null;
            }
        }

        // Add built-ins and Collect global info
        NTCEnv ntcEnv = new NTCEnv(null);
        for (SourceFile root : roots) {
            ntcEnv.addSourceFile(root.getContractName(), root);
            root.addBuiltIns();
            root.passScopeContext(null);
            if (!root.ntcGlobalInfo(ntcEnv, null)) {
                // doesn't typecheck
                return null;
            }
        }

        logger.debug("Current Contracts: " + ntcEnv.globalSymTab().getTypeSet());

        // Generate constraints
        for (SourceFile root : roots) {
            root.ntcGenCons(ntcEnv, null);
        }

        // Check using SHErrLoc and get a solution
        logger.debug("generating cons file for NTC");
        // constructors: all types
        // assumptions: none or relations between types
        // constraints
        if (!Utils.writeCons2File(ntcEnv.getTypeSet(), ntcEnv.getTypeRelationCons(), ntcEnv.cons(),
                outputFile, false)) {
            return roots;
        }
        boolean result = false;
        try {
            result = runSLC(ntcEnv.programMap(), outputFile.getAbsolutePath(), DEBUG);
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }

        return result ? roots : null;
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
            ContractSym contractSym = new ContractSym(root.getContractName(), root.getContract());
            // contractSym.name = ((SourceFile) root).getContractName();
            // contractSym.astNode = root;
            if (contractNames.contains(contractSym.getName())) {
                throw new RuntimeException("duplicate contract names");
                //TODO: duplicate contract names
            }
            contractNames.add(contractSym.getName());
            contractMap.add(contractSym.getName(), contractSym);
            //root.findPrincipal(principalSet);
        }

        logger.debug("contracts: \n" + contractMap.getTypeSet());
        int idx = 0;
        for (SourceFile root : roots) {
            ContractSym contractSym = (ContractSym) contractMap.lookup(contractNames.get(idx));
            ++idx;
            contractSym.symTab = new SymTab(contractMap);
            root.globalInfoVisit(contractSym);
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

    /**
     * Create ifc constraints for the signature of a contract.
     * TODO: more detailed doc
     */
    private static void buildSignatureConstraints(String contractName, VisitEnv env,
            String namespace, String curContractName) {
        List<Constraint> cons = env.cons;
        // List<Constraint> trustCons = env.trustCons;
        // String contractName = root.getContractName();//contractNames.get(fileIdx);
        ContractSym contractSym = env.getContract(contractName);
        logger.debug("current Contract: " + contractName + "\n" + contractSym + "\n"
                + env.curSymTab.getTypeSet());
        // generate trust relationship dec constraints

//        if (!namespace.equals("")) {
//            env.addTrustConstraint(
//                    new Constraint(new Inequality(namespace, Relation.EQ, namespace + "..this"),
//                            null, curContractName,
//                            "TODO"));
//        }
        // String ifNameContract = contractSym.getLabelNameContract();
        // String ifContract = contractSym.getLabelContract();
        if (namespace.equals(curContractName)) {
//            cons.add(new Constraint(new Inequality(ifNameContract, Relation.EQ, ifContract),
//                    Utils.placeholder(), contractName,
//                    "The Code integrity label of contract " + ifNameContract + " may be incorrect"));

            for (Assumption assumption : contractSym.assumptions()) {
                env.addTrustConstraint(new Constraint(
                        assumption.toInequality(),
                        assumption.location(), contractName,
                        "Static trust relationship"));
            }
        } else {
            // TODO: handle the assumptions of other contracts
        }

        env.setCurContract(contractSym);
        // env.curSymTab.setParent(env.globalSymTab);//TODO

        for (HashMap.Entry<String, FuncSym> funcPair : contractSym.symTab.getFuncs().entrySet()) {
            FuncSym func = funcPair.getValue();
            logger.debug("add func's sig constraints: [" + func.funcName + "]");
            //TODO: simplify
            namespace = "";
            String ifNameCallBeforeLabel = func.externalPcSLC();
            String ifNameCallAfterLabel = func.internalPcSLC();
            // String ifNameCallLockLabel = func.getLabelNameCallLock(namespace);
            String ifNameCallGammaLabel = func.getLabelNameCallGamma();
            String ifCallBeforeLabel = func.getCallPcLabel(namespace);
            String ifCallAfterLabel = func.getCallAfterLabel(namespace);
            String ifCallLockLabel = func.getCallLockLabel(namespace);
            // logger.debug(ifNameCallBeforeLabel + "\n" + ifNameCallAfterLabel + "\n" + ifNameCallLockLabel + "\n" + ifCallAfterLabel + "\n" +ifCallLockLabel);
            if (ifCallBeforeLabel != null) {
                cons.add(new Constraint(
                        new Inequality(ifCallBeforeLabel, Relation.EQ, ifNameCallBeforeLabel),
                        func.external_pc.location(), contractName,
                        "Integrity requirement to call this method may be incorrect"));
            }
            if (ifCallAfterLabel != null) {
                cons.add(new Constraint(
                        new Inequality(ifCallAfterLabel, Relation.EQ, ifNameCallAfterLabel),
                        func.internal_pc.location(), contractName,
                        "Integrity pc level autoendorsed to when calling this method may be incorrect"));
            }

            if (ifCallLockLabel != null) {
                cons.add(new Constraint(
                        new Inequality(ifCallLockLabel, Relation.EQ, ifNameCallGammaLabel),
                        func.gamma.location(), contractName,
                        "The final reentrancy lock label may be declared incorrectly"));

            }

            String ifNameReturnLabel = func.returnSLC();
            String ifReturnLabel = func.getRtnValueLabel(namespace);
            if (ifReturnLabel != null) {
                cons.add(new Constraint(
                        new Inequality(ifReturnLabel, Relation.EQ, ifNameReturnLabel),
                        func.location, contractName,
                        "Integrity label of this method's return value may be incorrect"));
            }

            for (int i = 0; i < func.parameters.size(); ++i) {
                VarSym arg = func.parameters.get(i);
                String ifNameArgLabel = func.getLabelNameArg(i);
//                String ifArgLabel = namespace + "." + arg.getLabelValueSLC();
                String ifArgLabel = arg.labelValueSLC();

                if (ifArgLabel != null) {
                    cons.add(new Constraint(new Inequality(ifNameArgLabel, Relation.EQ, ifArgLabel),
                            arg.labelLocation(), contractName,
                            "Argument " + arg.getName() + " may be labeled incorrectly"));
                    env.addTrustConstraint(
                            new Constraint(new Inequality(ifNameCallBeforeLabel, ifNameArgLabel),
                                    arg.labelLocation(), contractName,
                                    "Argument " + arg.getName()
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

        env.setCurContract(contractSym);
        // env.curSymTab.setParent(env.globalSymTab);//TODO
        env.cons = new ArrayList<>();

        logger.debug("Display varMap:");
        //System.err.println("Display varMap:\n");
        for (HashMap.Entry<String, VarSym> varPair : contractSym.symTab.getVars().entrySet()) {
            VarSym var = varPair.getValue();
            String varName = var.labelNameSLC();
            logger.debug(varName);
            String ifLabel = var.labelValueSLC();
            if (ifLabel != null && varName != null) {
                env.cons.add(
                        new Constraint(new Inequality(varName, Relation.EQ, ifLabel), var.location,
                                contractName,
                                "Variable " + var.getName() + " may be labeled incorrectly"));

                //env.cons.add(new Constraint(new Inequality(if Label, varName), var.location));

            }
            logger.debug(": {}", var);
            //System.err.println(varName);
            //System.err.println(": " + var + "\n");
        }

        //SigCons curSigCons = env.getSigCons(contractName);
        //env.trustCons.addAll(curSigCons.trustcons);
        //env.cons.addAll(curSigCons.cons);
        // System.out.println("before prinSet size: " + env.principalSet().size());

        SourceFile sourceFile = root;
        // env.varNameMap = new LookupMaps(varMap);

        sourceFile.genConsVisit(env, true);
        // System.out.println("mid prinSet size: " + env.principalSet().size());
        buildSignatureConstraints(sourceFile.getContractName(), env, sourceFile.getContractName(),
                sourceFile.getContractName());
        env.sigReq.forEach((name, curContractName) -> {
            buildSignatureConstraints(curContractName, env, name, sourceFile.getContractName());
        });

        // System.out.println("prinSet size: " + env.principalSet().size());
        if (!Utils.writeCons2File(env.principalSet(), env.trustCons(), env.cons, outputFile, true)) {
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

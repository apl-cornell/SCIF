import java.io.*;

import ast.*;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.Stream;
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
    public static List<SourceFile> regularTypecheck(List<File> inputFiles, File outputFile,
            boolean DEBUG) throws IOException {

        logger.trace("typecheck starts...");

        /*
            parse all SCIF source files and store AST roots in roots.
         */
        List<SourceFile> roots = new ArrayList<>();

        Queue<File> mentionedFiles = new ArrayDeque<>(inputFiles);
        InheritGraph graph = new InheritGraph();
        Map<String, SourceFile> fileMap = new HashMap<>();
        Set<String> includedFilePaths = inputFiles.stream().flatMap(file -> Stream.of(file.getAbsolutePath())).collect(
                Collectors.toSet());
        // add all built-in source files
        for (File builtinFile: Utils.BUILTIN_FILES) {
            Symbol result = Parser.parse(builtinFile, null);//p.parse();
            SourceFile root = ((SourceFile) result.value).makeBuiltIn();
            if (root instanceof ContractFile) {
                ((ContractFile) root).getContract().clearExtends();
            }
            // TODO root.setName(inputFile.name());
            List<String> sourceCode = Files.readAllLines(Paths.get(builtinFile.getAbsolutePath()),
                    StandardCharsets.UTF_8);
            root.setSourceCode(sourceCode);
            root.addBuiltIns();
            roots.add(root);
            assert root.ntcAddImportEdges(graph);
            includedFilePaths.add(builtinFile.getAbsolutePath());
            fileMap.put(builtinFile.getAbsolutePath(), root);
        }
        while (!mentionedFiles.isEmpty()) {
            File file = mentionedFiles.poll();
            Symbol result = Parser.parse(file, null);
            SourceFile root = (SourceFile) result.value;
            fileMap.put(root.getSourceFilePath(), root);
            // TODO root.setName(inputFile.name());
            List<String> sourceCode = Files.readAllLines(Paths.get(file.getAbsolutePath()),
                    StandardCharsets.UTF_8);
            root.setSourceCode(sourceCode);
            root.addBuiltIns();
            roots.add(root);
            assert root.ntcAddImportEdges(graph);

            for (String filePath: root.importPaths()) {
                if (!includedFilePaths.contains(filePath)) {
                    mentionedFiles.add(new File(filePath));
                    includedFilePaths.add(filePath);
                }
            }
        }


        // Code-paste superclasses' methods and data fields
        Map<String, Contract> contractMap = new HashMap<>();
        Map<String, Interface> interfaceMap = new HashMap<>();
        for (SourceFile root : roots) {
//            fileMap.put(root.getSourceFilePath(), root);
            // assert root.ntcAddImportEdges(graph);
//            System.err.println("sourcefile: " + root.getSourceFilePath());
            if (root instanceof ContractFile) {
                contractMap.put(root.getSourceFilePath(), ((ContractFile) root).getContract());
            } else if (root instanceof InterfaceFile) {
                interfaceMap.put(root.getSourceFilePath(), ((InterfaceFile) root).getInterface());
            } else {
                assert false: root.getContractName();
            }
        }

        logger.debug(
                " check if there is any non-existent contract name: " + contractMap.keySet() + " "
                        + graph.getAllNodes());
        // check if there is any non-existent contract name
        for (String contractPath : graph.getAllNodes()) {
            assert contractMap.containsKey(contractPath) || interfaceMap.containsKey(contractPath) : contractPath;
            /*if (!contractMap.containsKey(contractName)) {
                // TODO: mentioning non-existent contract
                return null;
            }*/
        }

        logger.debug(" code-paste in a topological order");
        List<SourceFile> toporder = new ArrayList<>();
        // code-paste in a topological order
        for (String x : graph.getTopologicalQueue()) {
            SourceFile rt = fileMap.get(x);
            if (rt == null) {
                // TODO: contract not found
                assert false;
                return null;
            }
            toporder.add(rt);
            rt.updateImports(fileMap);
            rt.codePasteContract(x, contractMap, interfaceMap);
        }
        roots = toporder;

        // Add built-ins and Collect global info
        NTCEnv ntcEnv = new NTCEnv(null);
        for (SourceFile root : roots) {
            ntcEnv.addSourceFile(root.getSourceFilePath(), root);

            root.passScopeContext(null);
            // System.err.println("Global checking " + root.getContractName());
            assert root.ntcGlobalInfo(ntcEnv, null): root.getContractName();
        }

        // Generate constraints
        for (SourceFile root : roots) {
            if (!root.isBuiltIn() && root instanceof ContractFile) {
                root.ntcGenCons(ntcEnv, null);
            }
        }

        // Check using SHErrLoc and get a solution
        logger.debug("generating cons file for NTC");
        // constructors: all types
        // assumptions: none or relations between types
        // constraints
        if (!Utils.writeCons2File(ntcEnv.getTypeSet(), ntcEnv.getTypeRelationCons(), ntcEnv.cons(),
                outputFile, false, null)) {
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

        Map<SourceFile, File> outputFileMap = new HashMap<>();
        for (int i = 0; i < roots.size(); ++i) {
            outputFileMap.put(roots.get(i), outputFiles.get(i));
        }

        // HashMap<String, ContractSym> contractMap = new HashMap<>();
        SymTab contractMap = new SymTab();
        List<String> contractNames = new ArrayList<>();
        //HashSet<String> principalSet = new HashSet<>();
        //env.principalSet.add("this");

        //ArrayList<Constraint> cons = new ArrayList<>();

        for (SourceFile root : roots) {
            if (root instanceof ContractFile) {
                ContractSym contractSym = new ContractSym(root.getContractName(),
                        ((ContractFile) root).getContract());
                // contractSym.name = ((SourceFile) root).getContractName();
                // contractSym.astNode = root;
                if (contractNames.contains(contractSym.getName())) {
                    throw new RuntimeException("duplicate contract names");
                    //TODO: duplicate contract names
                }
                contractNames.add(contractSym.getName());
                contractMap.add(contractSym.getName(), contractSym);
                //root.findPrincipal(principalSet);
            } else {
                InterfaceSym interfaceSym = new InterfaceSym(root.getContractName(),
                        ((InterfaceFile) root).getInterface());
                if (contractNames.contains(interfaceSym.getName())) {
                    throw new RuntimeException("duplicate contract names");
                    //TODO: duplicate contract names
                }
                contractNames.add(interfaceSym.getName());
                contractMap.add(interfaceSym.getName(), interfaceSym);
            }
        }

        logger.debug("contracts: \n" + contractMap.getTypeSet());
        int idx = 0;
        for (SourceFile root : roots) {
            InterfaceSym contractSym = (InterfaceSym) contractMap.lookup(contractNames.get(idx));
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

        for (SourceFile root : roots)
            if (root instanceof ContractFile) {
                env.programMap.put(root.getContractName(), root);
                env.sigReq.clear();
                if (!ifcTypecheck((ContractFile) root, env, outputFileMap.get(root), DEBUG)) {
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
        InterfaceSym contractSym = env.getContract(contractName);
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
                        assumption.location(),
                        "Static trust relationship"));
            }
        } else {
            // TODO: handle the assumptions of other contracts
        }

        env.setCurContract(contractSym);
        // env.curSymTab.setParent(env.globalSymTab);//TODO

        for (Map.Entry<String, FuncSym> funcPair : contractSym.symTab.getFuncs().entrySet()) {
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
                        func.external_pc.location(),
                        "Integrity requirement to call this method may be incorrect"));
            }
            if (ifCallAfterLabel != null) {
                cons.add(new Constraint(
                        new Inequality(ifCallAfterLabel, Relation.EQ, ifNameCallAfterLabel),
                        func.internal_pc.location(),
                        "Integrity pc level autoendorsed to when calling this method may be incorrect"));
            }

            if (ifCallLockLabel != null) {
                cons.add(new Constraint(
                        new Inequality(ifCallLockLabel, Relation.EQ, ifNameCallGammaLabel),
                        func.gamma.location(),
                        "The final reentrancy lock label may be declared incorrectly"));

            }

            String ifNameReturnLabel = func.returnSLC();
            String ifReturnLabel = func.getRtnValueLabel(namespace);
            if (ifReturnLabel != null) {
                cons.add(new Constraint(
                        new Inequality(ifReturnLabel, Relation.EQ, ifNameReturnLabel),
                        func.location,
                        "Integrity label of this method's return value may be incorrect"));
            }

            for (int i = 0; i < func.parameters.size(); ++i) {
                VarSym arg = func.parameters.get(i);
                String ifNameArgLabel = func.getLabelNameArg(i);
//                String ifArgLabel = namespace + "." + arg.getLabelValueSLC();
                String ifArgLabel = arg.labelValueSLC();

                if (ifArgLabel != null) {
                    cons.add(new Constraint(new Inequality(ifNameArgLabel, Relation.EQ, ifArgLabel),
                            arg.location,
                            "Argument " + arg.getName() + " may be labeled incorrectly"));
                    env.addTrustConstraint(
                            new Constraint(new Inequality(ifNameCallBeforeLabel, ifNameArgLabel),
                                    arg.location,
                                    "Argument " + arg.getName()
                                            + " must be no more trusted than caller's integrity"));
                }
            }

        }
        cons.add(new Constraint());
        // env.addSigCons(contractName, trustCons, cons);
    }

    private static boolean ifcTypecheck(ContractFile contractFile, VisitEnv env, File outputFile,
            boolean DEBUG) {
        String contractName = contractFile.getContractName();//contractNames.get(fileIdx);
        InterfaceSym contractSym = env.getContract(contractName);
        logger.debug("cururent Contract: " + contractName + "\n" + contractSym + "\n"
                + env.curSymTab.getTypeSet());

        env.setCurContract(contractSym);
        // env.curSymTab.setParent(env.globalSymTab);//TODO
        env.cons = new ArrayList<>();

        logger.debug("Display varMap:");
        for (Map.Entry<String, VarSym> varPair : contractSym.symTab.getVars().entrySet()) {
            VarSym var = varPair.getValue();
            String varName = var.labelNameSLC();
            logger.debug(varName);
            String ifLabel = var.labelValueSLC();
            if (ifLabel != null && varName != null) {
                env.cons.add(
                        new Constraint(new Inequality(varName, Relation.EQ, ifLabel), var.location,
                                "Variable " + var.getName() + " may be labeled incorrectly"));

                //env.cons.add(new Constraint(new Inequality(if Label, varName), var.location));

            }
            logger.debug(": {}", var);
        }

        contractFile.genConsVisit(env, true);
        buildSignatureConstraints(contractFile.getContractName(), env, contractFile.getContractName(),
                contractFile.getContractName());
        env.sigReq.forEach((name, curContractName) -> {
            buildSignatureConstraints(curContractName, env, name, contractFile.getContractName());
        });

        // System.out.println("prinSet size: " + env.principalSet().size());
        if (!Utils.writeCons2File(env.principalSet(), env.trustCons(), env.cons, outputFile, true, contractSym)) {
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

    static boolean runSLC(Map<String, SourceFile> programMap, String outputFileName,
            boolean DEBUG) throws Exception {
        logger.trace("running SLC");


        String classDirectoryPath = new File(
                SCIF.class.getProtectionDomain().getCodeSource().getLocation().toURI()).getPath();
        sherrloc.diagnostic.DiagnosticConstraintResult result = Utils.runSherrloc(outputFileName);
        logger.debug("runSLC: " + result);
        if (result.success()) {
            // System.out.println(Utils.TYPECHECK_PASS_MSG);
        } else {
            System.out.println(Utils.TYPECHECK_ERROR_MSG);
            double best = Double.MAX_VALUE;
            boolean seced = false;
            System.out.println("Places most likely to be wrong:");
            if (DEBUG) {
                System.out.println("No of places: " + result.getSuggestions().size());
            }
            int idx = 0;
            Set<String> expSet = new HashSet<>();
            for (int i = 0; i < result.getSuggestions().size(); ++i) {
                // if (i > 0) continue; // only output the first suggestion
                double weight = result.getSuggestions().get(i).getWeight();
                if (best > weight) {
                    best = weight;
                }
                if (!seced && best < weight) {
                    seced = true;
                    System.out.println("Some other possible places:");
                }
                List<String> s = Utils.SLCEntitiesToStrings(programMap, result.getSuggestions().get(i),
                        DEBUG);
                if (s != null) {
                    for (String exp: s) {
                        if (!expSet.contains(exp)) {
                            expSet.add(exp);
                            idx += 1;
                            System.out.println(idx + ":" + exp + "\n");
                        }
                    }
//                    System.out.println(idx + ":");
                }
            }
            return false;
        }
        return true;
    }

    protected static final Logger logger = LogManager.getLogger();
}

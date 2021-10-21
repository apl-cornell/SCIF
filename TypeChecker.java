import java.io.*;

import ast.*;
import java_cup.runtime.*;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import sherrlocUtils.Constraint;
import sherrlocUtils.Inequality;
import sherrlocUtils.Relation;
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

    public static ArrayList<Program> regularTypecheck(ArrayList<File> inputFiles, File outputFile) {

        logger.trace("typecheck starts");

        ArrayList<Program> roots = new ArrayList<>();
        for (File inputFile : inputFiles) {
            try {
                Lexer lexer = new Lexer(new FileReader(inputFile));
                Parser p = new Parser(lexer);
                Symbol result = p.parse();
                Program root = (Program) result.value;
                root.setProgramName(inputFile.getName());
                ArrayList<String> sourceCode = new ArrayList<String>(Files.readAllLines(Paths.get(inputFile.getAbsolutePath()), StandardCharsets.UTF_8));
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

        // Code-paste superclass'd methods and datafield
        HashMap<String, Contract> contractMap =  new HashMap<>();
        InheritGraph graph = new InheritGraph();
        for (Program root : roots) {
            if (!root.NTCinherit(graph)) {
                // TODO: doesn't typecheck
                return null;
            }
            if (root.getContract() != null) {
                contractMap.put(root.getContractName(), root.getContract());
            }
        }

        logger.debug(" check if there is any non-existent contract name: " + contractMap.keySet() + " " + graph.getAllNodes());
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
                if (((Program) root).containContract(x)) {
                    rt = root;
                    break;
                }
            }
            if (rt == null) {
                // TODO: contract not found
                return null;
            }
            if (!((Program) rt).codePasteContract(x, contractMap)) {
                // TODO: inherit failed
                return null;
            }
        }

        // Collect global info
        NTCEnv NTCenv = new NTCEnv();
        for (Node root : roots) {
            root.passScopeContext(null);
            if (!root.NTCGlobalInfo(NTCenv, null)) {
                // doesn't typecheck
                return null;
            }
        }

        logger.debug("Current Contracts: " + NTCenv.globalSymTab.getTypeSet());


        // Generate constraints
        for (Node root : roots) {
            root.NTCgenCons(NTCenv, null);
        }
        // Check using SHErrLoc and get a solution


        logger.debug("generating cons file for NTC");
        // constructors: all types
        // assumptions: none or relations between types
        // constraints
        Utils.writeCons2File(NTCenv.getTypeSet(), NTCenv.getTypeRelationCons(), NTCenv.cons, outputFile, false);
        boolean result = false;
        try {
            result = runSLC(roots.get(0), outputFile.getAbsolutePath());
        } catch (Exception e) {
            // handle exceptions;
        }

        return result == true ? roots : null;
    }

    public static boolean ifcTypecheck(ArrayList<Program> roots, ArrayList<File> outputFiles) {

        HashMap<Program, File> outputFileMap = new HashMap<>();
        for (int i = 0; i < roots.size(); ++i) {
            outputFileMap.put(roots.get(i), outputFiles.get(i));
        }

        // HashMap<String, ContractSym> contractMap = new HashMap<>();
        SymTab contractMap = new SymTab();
        ArrayList<String> contractNames = new ArrayList<>();
        //HashSet<String> principalSet = new HashSet<>();
        //env.principalSet.add("this");

        //ArrayList<Constraint> cons = new ArrayList<>();

        for (Program root : roots) {
            ContractSym contractSym = new ContractSym();
            contractSym.name = ((Program)root).getContractName();
            contractNames.add(contractSym.name);
            contractMap.add(contractSym.name, contractSym);
            //root.findPrincipal(principalSet);
        }

        logger.debug("contracts: \n" + contractMap.getTypeSet());
        int idx = 0;
        for (Program root : roots) {
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

        VisitEnv env = new VisitEnv();

        env.globalSymTab = env.curSymTab = contractMap;

        // collect constraints generating for signatures for each contract
        // a map from filename -> contract name -> signature info
        for (Program root : roots) {
            buildSignatureConstraints(root, env);
        }

        for (Program root : roots) {
            if (!ifcTypecheck(root, env, outputFileMap.get(root))) {
                return false;
            }
        }



        logger.trace("typecheck finishes");
        return true;
    }

    private static void buildSignatureConstraints(Program root, VisitEnv env) {
        ArrayList<Constraint> cons =  new ArrayList<>();
        ArrayList<Constraint> trustCons = new ArrayList<>();
        String contractName = root.getContractName();//contractNames.get(fileIdx);
        ContractSym contractSym = env.getContract(contractName);
        logger.debug("cururent Contract: " + contractName + "\n" + contractSym + "\n" + env.curSymTab.getTypeSet());
        // generate trust relationship dec constraints

        String ifNameContract = contractSym.getLabelNameContract();
        String ifContract = contractSym.getLabelContract();
        cons.add(new Constraint(new Inequality(ifNameContract, Relation.EQ, ifContract), contractSym.ifl.location, contractName,
                "Integrity label of contract " + ifNameContract + " is incorrect"));

        for (TrustConstraint trustConstraint : contractSym.trustSetting.trust_list) {
            trustCons.add(new Constraint(new Inequality(trustConstraint.lhs.toSherrlocFmt(), trustConstraint.optor,trustConstraint.rhs.toSherrlocFmt()), trustConstraint.location, contractName,
                    "Static trust relationship"));
        }


        // HashMap<String, VarSym> varMap = contractSym.varMap;
        // env.funcMap = contractSym.funcMap;
        env.curContractSym = contractSym;
        env.curSymTab = contractSym.symTab;
        env.curSymTab.setParent(env.globalSymTab);//TODO

        for (HashMap.Entry<String, FuncSym> funcPair : contractSym.symTab.getFuncs().entrySet()) {
            //String funcName = funcPair.getKey();
            FuncSym func = funcPair.getValue();
            //TODO: simplify
            String ifNameCallBeforeLabel = func.getLabelNameCallPcBefore();
            String ifNameCallAfterLabel = func.getLabelNameCallPcAfter();
            String ifNameCallLockLabel = func.getLabelNameCallLock();
            String ifNameCallGammaLabel = func.getLabelNameCallGamma();
            String ifCallBeforeLabel = func.getCallPcLabel();
            String ifCallAfterLabel = func.getCallAfterLabel();
            String ifCallLockLabel = func.getCallLockLabel();
            logger.debug("add func's sig constraints: [" + func.funcName + "]");
            logger.debug(ifNameCallBeforeLabel + "\n" + ifNameCallAfterLabel + "\n" + ifNameCallLockLabel + "\n" + ifCallAfterLabel + "\n" +ifCallLockLabel);
            // String ifAfterCallLabel = func.getCallAfterLabel();
            if (ifCallBeforeLabel != null) {
                cons.add(new Constraint(new Inequality(ifCallBeforeLabel, Relation.EQ, ifNameCallBeforeLabel), func.funcLabels.begin_pc.location, contractName,
                        "Integrity requirement to call this method is incorrect"));

                //env.cons.add(new Constraint(new Inequality(ifNameCallBeforeLabel, ifCallBeforeLabel), func.location));

            }
            if (ifCallAfterLabel != null) {
                cons.add(new Constraint(new Inequality(ifCallAfterLabel, Relation.EQ, ifNameCallAfterLabel), func.funcLabels.to_pc.location, contractName,
                        "Integrity pc level autoendorsed to when calling this method is incorrect"));

                //env.cons.add(new Constraint(new Inequality(ifNameCallAfterLabel, ifCallAfterLabel), func.location));
                // if (!ifCallAfterLabel.equals(ifCallBeforeLabel)) //TODO: deal with before and after are different
                // cons.add(new Constraint(new Inequality(ifNameCallAfterLabel, Relation.GEQ, ifNameCallLockLabel), func.funcLabels.to_pc.location, contractName, "Calls to this function must respect lock level of " + '{' + ifCallAfterLabel + '}'));
            }

            if (ifCallLockLabel != null) {
                // cons.add(new Constraint(new Inequality(ifCallLockLabel, Relation.REQ, ifNameCallLockLabel), func.funcLabels.gamma_label.location, contractName, "Calls to this function must respect lock level of " + '{' + ifCallAfterLabel + '}'));
                cons.add(new Constraint(new Inequality(ifCallLockLabel, Relation.EQ, ifNameCallGammaLabel), func.funcLabels.gamma_label.location, contractName,
                        "The final lock label is declared incorrectly"));

            }

            String ifNameReturnLabel = func.getLabelNameRtnValue();
            String ifReturnLabel = func.getRtnValueLabel();
            // String ifNameRtnLockLabel = func.getLabelNameRtnLock();
            // String ifAfterCallLabel = func.getCallAfterLabel();
            // String ifRtnLockLabel = func.getRtnLockLabel();
            if (ifReturnLabel != null) {
                cons.add(new Constraint(new Inequality(ifReturnLabel, Relation.EQ, ifNameReturnLabel), func.location, contractName,
                        "Integrity label of this method's return value is incorrect"));

                //env.cons.add(new Constraint(new Inequality(ifNameReturnLabel, ifReturnLabel), func.location));

            }
            /*if (ifRtnLockLabel != null) {
                env.cons.add(new Constraint(new Inequality(ifNameRtnLockLabel, ifRtnLockLabel), func.location));
            }
            if (ifAfterCallLabel != null) {
                env.cons.add(new Constraint(new Inequality(ifNameRtnLockLabel, ifAfterCallLabel), func.location));
            }*/
            /*if (ifCallLockLabel != null && ifCallAfterLabel != null && ifCallLockLabel.equals(ifCallAfterLabel)) {
                cons.add(new Constraint(new Inequality(ifCallLockLabel, Relation.EQ, ifNameRtnLockLabel), func.funcLabels.location, contractName,
                        "Calls to this method must respect label " + '{' + ifCallLockLabel + '}'));

            } else {
                if (ifCallLockLabel != null) {
                    cons.add(new Constraint(new Inequality(ifCallLockLabel, ifNameRtnLockLabel), func.funcLabels.location, contractName,
                            "Calls to this method must respect label " + '{' + ifCallLockLabel + '}'));
                }
                if (ifCallAfterLabel != null) {
                    cons.add(new Constraint(new Inequality(ifCallAfterLabel, ifNameRtnLockLabel), func.funcLabels.location, contractName,
                            "Calls to this method must respect label " + '{' + ifCallAfterLabel + '}'));
                }
            }*/

            for (int i = 0; i < func.parameters.size(); ++i) {
                VarSym arg = func.parameters.get(i);
                String ifNameArgLabel = func.getLabelNameArg(i);
                String ifArgLabel = arg.getLabel();
                if (ifArgLabel != null) {
                    cons.add(new Constraint(new Inequality(ifNameArgLabel, Relation.EQ, ifArgLabel), arg.location, contractName,
                            "Argument " + arg.name + " is labeled incorrectly"));

                    //env.cons.add(new Constraint(new Inequality(ifArgLabel, ifNameArgLabel), arg.location));

                }
            }

        }
        cons.add(new Constraint());
        env.addSigCons(contractName, trustCons, cons);
    }

    private static boolean ifcTypecheck(Program root, VisitEnv env, File outputFile) {
        String contractName = root.getContractName();//contractNames.get(fileIdx);
        ContractSym contractSym = env.getContract(contractName);
        logger.debug("cururent Contract: " + contractName + "\n" + contractSym + "\n" + env.curSymTab.getTypeSet());

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
                env.cons.add(new Constraint(new Inequality(varName, Relation.EQ, ifLabel), var.location, contractName,
                        "Variable " + var.name + " is labeled incorrectly"));

                //env.cons.add(new Constraint(new Inequality(if Label, varName), var.location));

            }
            logger.debug(varName);
            logger.debug(": {}", var);
            //System.err.println(varName);
            //System.err.println(": " + var + "\n");
        }

        SigCons curSigCons = env.getSigCons(contractName);
        env.trustCons.addAll(curSigCons.trustcons);
        env.cons.addAll(curSigCons.cons);

        Node tmp = root;
        if (!(tmp instanceof Program))  {
            // TODO: not supported currently
            Interface anInterface = (Interface) tmp;
            for (FunctionSig functionSig : anInterface.funcSigs) {
                functionSig.findPrincipal(env.principalSet);
            }

        } else {
            Program program = (Program) tmp;
            // env.varNameMap = new LookupMaps(varMap);

            program.genConsVisit(env, true);
        }

        Utils.writeCons2File(env.principalSet, env.trustCons, env.cons, outputFile, true);
        boolean result = false;
        try {
            result = runSLC(root, outputFile.getAbsolutePath());
        } catch (Exception e) {
            // handle exceptions;
        }
        return result;
    }

    static boolean runSLC(Program root, String outputFileName) throws Exception {

        String classDirectoryPath = new File(SCIF.class.getProtectionDomain().getCodeSource().getLocation().toURI()).getPath();
        String[] sherrlocResult = Utils.runSherrloc(classDirectoryPath, outputFileName);
        logger.debug(sherrlocResult);
        if (sherrlocResult.length < 1) {
            System.out.println(Utils.TYPECHECK_NORESULT_MSG);
            return false;
        } else if (sherrlocResult[sherrlocResult.length - 1].contains(Utils.SHERRLOC_PASS_INDICATOR)) {
            System.out.println(Utils.TYPECHECK_PASS_MSG);
        } else {
            System.out.println(Utils.TYPECHECK_ERROR_MSG);
            for (int i = 0; i < sherrlocResult.length; ++i)
                if (sherrlocResult[i].contains(Utils.SHERRLOC_ERROR_INDICATOR)) {
                    System.out.println("Places most likely to be wrong:");
                    int idx = 1;
                    for (int j = i + 1; j < sherrlocResult.length; ++j) {
                        // System.out.println(sherrlocResult[j]);
                        //if (true)
                          //  continue;
                        String s = Utils.translateSLCSuggestion(root, sherrlocResult[j]);
                        if (s != null) {
                            System.out.println(idx + ":");
                            System.out.println(s + "\n");
                            idx += 1;
                        }
                    }
                    break;
                }
            return false;
        }
        return true;
    }

    protected static final Logger logger = LogManager.getLogger();
}

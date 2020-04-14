import java.io.*;

import ast.*;
import java_cup.*;
import java_cup.runtime.*;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import sherrlocUtils.Constraint;
import sherrlocUtils.Inequality;
import sherrlocUtils.Relation;
import typecheck.*;

import java.util.*;

public class TypeChecker {
    public static void main(String[] args) {
        File inputFile = new File(args[0]);
        File outputFile = new File(args[1]);
        //typecheck(inputFile, outputFile);
    }

    public static ArrayList<Node> typecheck(ArrayList<File> inputFiles, File outputFile) {

        logger.trace("typecheck starts");

        ArrayList<Node> roots = new ArrayList<>();
        for (File inputFile : inputFiles) {
            try {
                Lexer lexer = new Lexer(new FileReader(inputFile));
                Parser p = new Parser(lexer);
                Symbol result = p.parse();
                Node root = (Node) result.value;
                roots.add(root);
                logger.debug("Finish");
                //System.err.println("Finish\n");
            } catch (Exception e) {
                e.printStackTrace();
                return null;
            }
        }

        // Step 1: typecheck, generate constraints and check via SHErrLoc

        // Collect global info
        NTCEnv NTCenv = new NTCEnv();
        for (Node root : roots) {
            root.passScopeContext(null);
            if (!root.NTCGlobalInfo(NTCenv, null)) {
                // doesn't typecheck
                return null;
            }
        }

        // Generate constraints
        for (Node root : roots) {
            root.NTCgenCons(NTCenv, null);
        }

        // Check using SHErrLoc and get a solution

        // HashMap<String, ContractSym> contractMap = new HashMap<>();
        SymTab contractMap = new SymTab();
        ArrayList<String> contractNames = new ArrayList<>();
        //HashSet<String> principalSet = new HashSet<>();
        //env.principalSet.add("this");

        //ArrayList<Constraint> cons = new ArrayList<>();

        for (Node root : roots) {
            ContractSym contractSym = new ContractSym();
            root.globalInfoVisit(contractSym);
            if (contractNames.contains(contractSym.name)) {
                //TODO: duplicate contract names
            }
            logger.debug(contractSym.toString());
            contractNames.add(contractSym.name);
            contractMap.add(contractSym.name, contractSym);
            //root.findPrincipal(principalSet);
        }

        logger.debug("generating cons file for NTC");
        // constructors: all types
        // assumptions: none or relations between types
        // constraints
        Utils.writeCons2File(NTCenv.getTypeSet(), NTCenv.getTypeRelationCons(), NTCenv.cons, outputFile);
        if (true) return roots;

        logger.debug("starting to ifc typecheck");

        VisitEnv env = new VisitEnv();

        env.globalSymTab = env.curSymTab = contractMap;

        for (int fileIdx = 0; fileIdx < roots.size(); ++fileIdx) {
            String contractName = contractNames.get(fileIdx);
            ContractSym contractSym = env.getContract(contractName);
            // generate trust relationship dec constraints
            for (TrustConstraint trustConstraint : contractSym.trustCons) {
                env.trustCons.add(new Constraint(new Inequality(trustConstraint.lhs.toSherrlocFmt(), trustConstraint.optor,trustConstraint.rhs.toSherrlocFmt()), trustConstraint.location));
            }


            // HashMap<String, VarSym> varMap = contractSym.varMap;
            // env.funcMap = contractSym.funcMap;
            env.curContractSym = contractSym;
            env.globalSymTab = env.curSymTab = contractSym.symTab; //TODO

            logger.debug("Display varMap:");
            //System.err.println("Display varMap:\n");
            for (HashMap.Entry<String, VarSym> varPair : contractSym.symTab.getVars().entrySet()) {
                VarSym var = varPair.getValue();
                String varName = var.labelToSherrlocFmt();
                String ifLabel = var.getLabel();
                if (ifLabel != null) {
                    env.cons.add(new Constraint(new Inequality(varName, Relation.EQ, ifLabel), var.location));

                    //env.cons.add(new Constraint(new Inequality(if Label, varName), var.location));

                }
                logger.debug(varName);
                logger.debug(": {}", var);
                //System.err.println(varName);
                //System.err.println(": " + var + "\n");
            }
            for (HashMap.Entry<String, FuncSym> funcPair : contractSym.symTab.getFuncs().entrySet()) {
                //String funcName = funcPair.getKey();
                FuncSym func = funcPair.getValue();
                //TODO: simplify
                String ifNameCallBeforeLabel = func.getLabelNameCallPc();
                String ifNameCallAfterLabel = func.getLabelNameCallPc();
                String ifNameCallLockLabel = func.getLabelNameCallLock();
                String ifCallBeforeLabel = func.getCallPcLabel();
                String ifCallAfterLabel = func.getCallPcLabel();
                String ifCallLockLabel = func.getCallLockLabel();
                if (ifCallBeforeLabel != null) {
                    env.cons.add(new Constraint(new Inequality(ifCallBeforeLabel, Relation.EQ, ifNameCallBeforeLabel), func.location));

                    //env.cons.add(new Constraint(new Inequality(ifNameCallBeforeLabel, ifCallBeforeLabel), func.location));

                }
                if (ifCallAfterLabel != null) {
                    env.cons.add(new Constraint(new Inequality(ifCallAfterLabel, Relation.EQ, ifNameCallAfterLabel), func.location));

                    //env.cons.add(new Constraint(new Inequality(ifNameCallAfterLabel, ifCallAfterLabel), func.location));

                }
                if (ifCallLockLabel != null) {
                    env.cons.add(new Constraint(new Inequality(ifCallLockLabel, Relation.EQ, ifNameCallLockLabel), func.location));
                }

                String ifNameReturnLabel = func.getLabelNameRtnValue();
                String ifReturnLabel = func.getRtnValueLabel();
                String ifNameRtnLockLabel = func.getLabelNameRtnLock();
                String ifRtnLockLabel = func.getRtnLockLabel();
                if (ifReturnLabel != null) {
                    env.cons.add(new Constraint(new Inequality(ifReturnLabel, Relation.EQ, ifNameReturnLabel), func.location));

                    //env.cons.add(new Constraint(new Inequality(ifNameReturnLabel, ifReturnLabel), func.location));

                }
                if (ifRtnLockLabel != null) {
                    env.cons.add(new Constraint(new Inequality(ifRtnLockLabel, Relation.EQ, ifNameRtnLockLabel), func.location));
                }

                for (int i = 0; i < func.parameters.size(); ++i) {
                    VarSym arg = func.parameters.get(i);
                    String ifNameArgLabel = func.getLabelNameArg(i);
                    String ifArgLabel = arg.getLabel();
                    if (ifArgLabel != null) {
                        env.cons.add(new Constraint(new Inequality(ifNameArgLabel, Relation.EQ, ifArgLabel), arg.location));

                        //env.cons.add(new Constraint(new Inequality(ifArgLabel, ifNameArgLabel), arg.location));

                    }
                }

            }
            env.cons.add(new Constraint());


            Node tmp = roots.get(fileIdx);
            if (!(tmp instanceof Program))  {
                Interface root = (Interface) tmp;
                for (FunctionSig functionSig : root.funcSigs) {
                    functionSig.findPrincipal(env.principalSet);
                }

            } else {
                Program root = (Program) tmp;
                // env.varNameMap = new LookupMaps(varMap);

                root.genConsVisit(env);
            }

        }

        Utils.writeCons2File(env.principalSet, env.trustCons, env.cons, outputFile);

        logger.trace("typecheck finishes");

        return roots;
    }

    protected static final Logger logger = LogManager.getLogger();
}

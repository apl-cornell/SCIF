import java.io.*;

import ast.FunctionSig;
import ast.Interface;
import ast.Node;
import ast.Program;
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

    public static void typecheck(ArrayList<File> inputFiles, File outputFile) {

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
                return;
            }
        }

        HashMap<String, ContractInfo> contractMap = new HashMap<>();
        ArrayList<String> contractNames = new ArrayList<>();
        //HashSet<String> principalSet = new HashSet<>();
        //env.principalSet.add("this");

        //ArrayList<Constraint> cons = new ArrayList<>();

        for (Node root : roots) {
            ContractInfo contractInfo = new ContractInfo();
            root.globalInfoVisit(contractInfo);
            if (contractNames.contains(contractInfo.name)) {
                //TODO: duplicate contract names
            }
            logger.debug(contractInfo.toString());
            contractNames.add(contractInfo.name);
            contractMap.put(contractInfo.name, contractInfo);
            //root.findPrincipal(principalSet);
        }


        VisitEnv env = new VisitEnv();

        env.contractMap = contractMap;

        for (int fileIdx = 0; fileIdx < roots.size(); ++fileIdx) {
            String contractName = contractNames.get(fileIdx);
            ContractInfo contractInfo = env.contractMap.get(contractName);
            HashMap<String, VarInfo> varMap = contractInfo.varMap;
            env.funcMap = contractInfo.funcMap;
            env.contractInfo = contractInfo;

            logger.debug("Display varMap:");
            //System.err.println("Display varMap:\n");
            for (HashMap.Entry<String, VarInfo> varPair : varMap.entrySet()) {
                VarInfo var = varPair.getValue();
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
            for (HashMap.Entry<String, FuncInfo> funcPair : env.funcMap.entrySet()) {
                //String funcName = funcPair.getKey();
                FuncInfo func = funcPair.getValue();
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
                    VarInfo arg = func.parameters.get(i);
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
                env.varNameMap = new LookupMaps(varMap);

                root.genConsVisit(env);
            }
        }
        try {
            BufferedWriter consFile = new BufferedWriter(new FileWriter(outputFile));
            logger.debug("Writing the constraints of size {}", env.cons.size());
            //System.err.println("Writing the constraints of size " + env.cons.size());
            if (!env.principalSet.isEmpty()) {
                for (String principal : env.principalSet) {
                    consFile.write("CONSTRUCTOR " + principal + " 0\n");
                }
            }
            consFile.write("\n");
            if (!env.cons.isEmpty()) {
                for (Constraint con : env.cons) {
                    consFile.write(con.toSherrlocFmt() + "\n");
                }
            }
            consFile.close();
        } catch (Exception e) {
            e.printStackTrace();
            return;
        }

        logger.trace("typecheck finishes");
    }

    protected static final Logger logger = LogManager.getLogger();
}

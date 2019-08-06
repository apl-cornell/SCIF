import java.io.*;

import ast.Program;
import java_cup.runtime.*;
import sherrlocUtils.Constraint;
import sherrlocUtils.Inequality;
import typecheck.*;

import java.util.*;

public class TypeChecker {
    public static void main(String[] args) {
        File inputFile = new File(args[0]);
        File outputFile = new File(args[1]);
        typecheck(inputFile, outputFile);
    }

    public static void typecheck(File inputFile, File outputFile) {
        Program root;
        try {
            Lexer lexer = new Lexer(new FileReader(inputFile));
            Parser p = new Parser(lexer);
            Symbol result = p.parse();
            root = (Program) result.value;
            System.err.println("Finish\n");
        } catch(Exception e) {
            e.printStackTrace();
            return;
        }
        HashMap<String, VarInfo> varMap = new HashMap<String, VarInfo>();
        VisitEnv env = new VisitEnv();
        //HashSet<String> principalSet = new HashSet<>();
        //env.principalSet.add("this");

        //ArrayList<Constraint> cons = new ArrayList<>();

        root.globalInfoVisit(varMap, env.funcMap);
        //root.findPrincipal(principalSet);

        System.err.println("Display varMap:\n");
        for (HashMap.Entry<String, VarInfo>  varPair : varMap.entrySet()) {
            VarInfo var = varPair.getValue();
            String varName = var.labelToSherrlocFmt();
            String ifLabel = var.getLabel();
            if (ifLabel != null) {
                env.cons.add(new Constraint(new Inequality(varName, ifLabel), var.location));

                env.cons.add(new Constraint(new Inequality(ifLabel, varName), var.location));

            }
            System.err.println(varName);
            System.err.println(": " + var + "\n");
        }
        for (HashMap.Entry<String, FuncInfo> funcPair : env.funcMap.entrySet()) {
            //String funcName = funcPair.getKey();
            FuncInfo func = funcPair.getValue();
            String ifNameCallBeforeLabel = func.getLabelNameCallBefore();
            String ifNameCallAfterLabel = func.getLabelNameCallAfter();
            String ifCallBeforeLabel = func.getCallBeforeLabel();
            String ifCallAfterLabel = func.getCallAfterLabel();
            if (ifNameCallBeforeLabel != null) {
                env.cons.add(new Constraint(new Inequality(ifCallBeforeLabel, ifNameCallBeforeLabel), func.location));

                env.cons.add(new Constraint(new Inequality(ifNameCallBeforeLabel, ifCallBeforeLabel), func.location));

            }
            if (ifNameCallAfterLabel != null) {
                env.cons.add(new Constraint(new Inequality(ifCallAfterLabel, ifNameCallAfterLabel), func.location));

                env.cons.add(new Constraint(new Inequality(ifNameCallAfterLabel, ifCallAfterLabel), func.location));

            }

            String ifNameReturnLabel = func.getLabelNameReturn();
            String ifReturnLabel = func.getReturnLabel();
            if (ifReturnLabel != null) {
                env.cons.add(new Constraint(new Inequality(ifReturnLabel, ifNameReturnLabel), func.location));

                env.cons.add(new Constraint(new Inequality(ifNameReturnLabel, ifReturnLabel), func.location));

            }

            for (int i = 0; i < func.parameters.size(); ++i) {
                VarInfo arg = func.parameters.get(i);
                String ifNameArgLabel = func.getLabelNameArg(i);
                String ifArgLabel = arg.getLabel();
                if (ifArgLabel != null) {
                    env.cons.add(new Constraint(new Inequality(ifNameArgLabel, ifArgLabel), arg.location));

                    env.cons.add(new Constraint(new Inequality(ifArgLabel, ifNameArgLabel), arg.location));

                }
            }

        }
        env.cons.add(new Constraint());

        env.varNameMap = new LookupMaps(varMap);

        root.genConsVisit(env);

        try {
            BufferedWriter consFile = new BufferedWriter(new FileWriter(outputFile));
            System.err.println("Writing the constraints of size " + env.cons.size());
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
        } catch(Exception e) {
            e.printStackTrace();
            return;
        }
    }


}

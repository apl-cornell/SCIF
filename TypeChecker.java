import java.io.*;

import ast.Program;
import java_cup.runtime.*;
import utils.*;

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
        HashMap<String, FuncInfo> funcMap = new HashMap<String, FuncInfo>();
        HashSet<String> principalSet = new HashSet<>();
        principalSet.add("this");

        ArrayList<IfConstraint> cons = new ArrayList<>();

        root.globalInfoVisit(varMap, funcMap);
        root.findPrincipal(principalSet);

        System.err.println("Display varMap:\n");
        for (HashMap.Entry<String, VarInfo>  varPair : varMap.entrySet()) {
            String varName = varPair.getKey();
            VarInfo var = varPair.getValue();
            String ifLabel = var.getLabel();
            if (ifLabel != null) {
                cons.add(Utils.genCons(varName, ifLabel, var.location));
                cons.add(Utils.genCons(ifLabel, varName, var.location));
            }
            System.out.println(varName);
            System.out.println(": " + var + "\n");
        }
        for (HashMap.Entry<String, FuncInfo> funcPair : funcMap.entrySet()) {
            //String funcName = funcPair.getKey();
            FuncInfo func = funcPair.getValue();
            String ifNameCallBeforeLabel = func.getLabelNameCallBefore();
            String ifNameCallAfterLabel = func.getLabelNameCallAfter();
            String ifCallBeforeLabel = func.getCallBeforeLabel();
            String ifCallAfterLabel = func.getCallAfterLabel();
            if (ifNameCallBeforeLabel != null) {
                cons.add(Utils.genCons(ifCallBeforeLabel, ifNameCallBeforeLabel, func.location));
                cons.add(Utils.genCons(ifNameCallBeforeLabel, ifCallBeforeLabel, func.location));
            }
            if (ifNameCallAfterLabel != null) {
                cons.add(Utils.genCons(ifCallAfterLabel, ifNameCallAfterLabel, func.location));
                cons.add(Utils.genCons(ifNameCallAfterLabel, ifCallAfterLabel, func.location));
            }

            String ifNameReturnLabel = func.getLabelNameReturn();
            String ifReturnLabel = func.getReturnLabel();
            if (ifReturnLabel != null) {
                cons.add(Utils.genCons(ifReturnLabel, ifNameReturnLabel, func.location));
                cons.add(Utils.genCons(ifNameReturnLabel, ifReturnLabel, func.location));
            }

            for (int i = 0; i < func.parameters.size(); ++i) {
                VarInfo arg = func.parameters.get(i);
                String ifNameArgLabel = func.getLabelNameArg(i);
                String ifArgLabel = arg.getLabel();
                if (ifArgLabel != null) {
                    cons.add(Utils.genCons(ifNameArgLabel, ifArgLabel, arg.location));
                    cons.add(Utils.genCons(ifArgLabel, ifNameArgLabel, arg.location));
                }
            }

        }
        cons.add(Utils.genNewlineCons());

        LookupMaps varNameMap = new LookupMaps(varMap);

        root.genConsVisit("", funcMap, cons, varNameMap);

        try {
            BufferedWriter consFile = new BufferedWriter(new FileWriter(outputFile));
            System.err.println("Writing the constraints of size " + cons.size());
            for (String principal : principalSet) {
                consFile.write("CONSTRUCTOR " + principal + " 0\n");
            }
            consFile.write("\n");
            for (IfConstraint con : cons) {
                consFile.write(con.toSherrlocFmt() + "\n");
            }
            consFile.close();
        } catch(Exception e) {
            e.printStackTrace();
            return;
        }
    }


}

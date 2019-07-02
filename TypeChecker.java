import java.io.*;

import ast.Program;
import java_cup.runtime.*;
import utils.*;

import java.util.*;

public class TypeChecker {
    public static void main(String[] args) {
        Program root;
        try {
            Lexer lexer = new Lexer(new FileReader(args[0]));
            Parser p = new Parser(lexer);
            Symbol result = p.parse();
            root = (Program) result.value;
            //Parser.printTree(root, 0);
            System.out.println("Finish\n");
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

        //collectGlobalDec("", root, varMap, funcMap);

        System.out.println("Display varMap:\n");
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
            String funcName = funcPair.getKey();
            FuncInfo func = funcPair.getValue();
            String ifNameCallLabel = func.getIfNameCallLabel();
            String ifCallLabel = func.getCallLabel();
            if (ifCallLabel != null) {
                cons.add(Utils.genCons(ifCallLabel, ifNameCallLabel, func.location));
                cons.add(Utils.genCons(ifNameCallLabel, ifCallLabel, func.location));
            }

            String ifNameReturnLabel = func.getIfNameReturnLabel();
            String ifReturnLabel = func.getReturnLabel();
            if (ifReturnLabel != null) {
                cons.add(Utils.genCons(ifReturnLabel, ifNameReturnLabel, func.location));
                cons.add(Utils.genCons(ifNameReturnLabel, ifReturnLabel, func.location));
            }

            for (int i = 0; i < func.parameters.size(); ++i) {
                VarInfo arg = func.parameters.get(i);
                String ifNameArgLabel = func.getIfNameArgLabel(i);
                String ifArgLabel = arg.getLabel();
                if (ifArgLabel != null) {
                    cons.add(Utils.genCons(ifNameArgLabel, ifArgLabel, arg.location));
                    cons.add(Utils.genCons(ifArgLabel, ifNameArgLabel, arg.location));
                }
            }

        }
        cons.add(Utils.genNewlineCons());
        //generate constraint from gloabl dec
/*        Iterator it = varMap.entrySet().iterator();
        while (it.hasNext()) {
            HashMap.Entry<String, utils.VarInfo> pair = (HashMap.Entry<String, utils.VarInfo>) it.next();
            utils.VarInfo v = (utils.VarInfo) pair.getValue();
            cons.add(utils.genCons(v.name, v.lbl, v.x, v.y));
            cons.add(utils.genCons(v.lbl, v.name, v.x, v.y));
        }*/
/*        it = funcMap.entrySet().iterator();
        while (it.hasNext()) {
            HashMap<String, utils.FuncInfo>.Entry pair = (HashMap<String, utils.FuncInfo>.Entry) it.next();
            utils.FuncInfo f = (utils.FuncInfo) pair.getValue();
            //cons.add(utils.genCons(f.name + "..call", f.callLbl, f.x, f.y));
            //cons.add(utils.genCons(f.name + "..return", f.rtLbl, f.x, f.y));
            Iterator iit = f.prmters.entrySet().iterator();
            while (iit.hasNext()) {
                HashMap<String. utils.VarInfo>.Entry pair = (HashMap<String, utils.VarInfo>.Entry) it.next();
                utils.VarInfo v = (utils.VarInfo) pair.getValue();
                cons.add(utils.genCons(f.name + "." + v.name, v.lbl, v.x, v.y));
                cons.add(utils.genCons(v.lbl, f.name + "." + v.name, v.x, v.y));
            }
        }*/

        LookupMaps varNameMap = new LookupMaps(varMap);

        root.genConsVisit("", funcMap, cons, varNameMap);

        //generateConstraints("", root, varMap, funcMap, cons, varNameMap);
        try {
            BufferedWriter consFile = new BufferedWriter(new FileWriter(args[1]));
            System.out.println("Writing the constraints of size " + cons.size());
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

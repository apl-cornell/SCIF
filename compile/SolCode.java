package compile;

import ast.*;
import java.util.List;
import typecheck.*;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.logging.Logger;

public class SolCode {

    int indentWidth;
    int indentLevel;
    String unitIndent;
    String currentIndent;
    public List<String> code; //each line with no newline char
    public HashMap<String, String> labelTable; // map label names to on-chain addresses
    public typecheck.DynamicSystemOption dynamicSystemOption;

    public SolCode() {
        code = new ArrayList<>();
        labelTable = new HashMap<>();
        labelTable.put("this", "address(this)");
        indentWidth = 4;
        indentLevel = 0;
        unitIndent = "";
        for (int i = 0; i < indentWidth; ++i) {
            unitIndent += " ";
        }
        currentIndent = "";
    }

    void addIndent() {
        indentLevel += 1;
        currentIndent += unitIndent;
    }

    void decIndent() {
        indentLevel -= 1;
        currentIndent = currentIndent.substring(0, currentIndent.length() - indentWidth);
    }

    public void addLine(String line) {
        // System.err.println("ADDING: " + currentIndent + line);
        if (line.equals("assert(true);") || line.equals("assert(!false);")) {
            line = "//" + line;
        }
        code.add(currentIndent + line);
    }

    public void addVersion(String version) {
        addLine("pragma solidity >=" + version + ";");
    }

    public void addImport(String contractName) {
        addLine("import \"./" + contractName + ".sol\";");
    }

    public void addAssign(String target, String value) {
        addLine(target + " = " + value + ";");
    }

    public void addBreak() {
        addLine("break;");
    }

    public void addReturn(String value) {
        addLine("return " + value + ";");
    }

    public void enterContractDef(String contractName) {
        addLine("contract " + contractName + " is BaseContractCentralized {");
        addIndent();
    }

    public void leaveContractDef() {
        decIndent();
        addLine("}");
    }

    public void enterFunctionDef(String name, String args, String returnType, boolean isPublic,
            boolean isPayable) {
        if (name.equals(Utils.CONSTRUCTOR_NAME)) {
            addLine("constructor (" + args + ")");
        } else {
            addLine("function " + name + "(" + args + ")");
        }
        addIndent();
        if (isPublic) {
            addLine(Utils.PUBLIC_DECORATOR);
        } else {
            addLine(Utils.PRIVATE_DECORATOR);
        }
        if (isPayable) {
            addLine(Utils.PAYABLE_DECORATOR);
        }
        if (!returnType.isEmpty()) {
            addLine("returns (" + returnType + ")");
        }
        decIndent();
        addLine("{");
        addIndent();
    }

    public void enterConstructorDef(String args, List<Statement> body) {
        addLine("constructor (" + args + ")");
        if (dynamicSystemOption == DynamicSystemOption.BaseContractCentralized) {
            boolean foundOption = false;
            addIndent();
            for (Statement stmt : body) {
                if (stmt instanceof DynamicStatement) {
                    String inherit = ((DynamicStatement) stmt).toSolCode();
                    addLine(inherit);
                    foundOption = true;
                    break; //TODO: handle duplicate calls
                }
            }
            if (!foundOption) {
                //TODO: error report
                addLine("No constructor for Dynamic System setting found");
            }
            decIndent();
        }
        addLine("{");
        addIndent();
    }

    public void leaveFunctionDef() {
        decIndent();
        addLine("}");
    }

    public void enterIf(String cond) {
        addLine("if (" + cond + ") {");
        addIndent();
    }

    public void leaveIf() {
        decIndent();
        addLine("}");
    }

    public void enterElse() {
        addLine("else {");
        addIndent();
    }

    public void leaveElse() {
        decIndent();
    }

    public void enterWhile(String cond) {
        addLine("while (" + cond + ") {");
        addIndent();
    }

    public void leaveWhile() {
        decIndent();
    }

    public void addVarDef(String type, String name, boolean isConst, String value) {
        addLine(type + " " + name + " = " + value + ";");
    }

    public void addVarDef(String type, String name, boolean isConst) {
        addLine(type + " " + name + ";");
    }

    public static String toAttribute(String value, String attr) {
        return value + "." + attr;
    }

    public static String toBinOp(String left, String op, String right) {
        return "(" + left + " " + op + " " + right + ")";
    }

    public static String toBoolOp(String left, String op, String right) {
        return "(" + left + " " + op + " " + right + ")";
    }

    public static String toCompareOp(String left, String op, String right) {
        return "(" + left + " " + op + " " + right + ")";
    }

    public static String toFunctionCall(String name, String args) {
        return name + "(" + args + ")";
    }

    public void output(File outputFile) {
        try {
            BufferedWriter codeFile = new BufferedWriter(new FileWriter(outputFile));
            for (String line : code) {
                codeFile.write(line + "\n");
            }
            codeFile.close();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void display() {
        for (String line : code) {
            System.out.println(line);
        }
    }

    public void enterFuncCheck(FuncLabels funcLabels, Arguments args) {
        /*
            f{pc_f->pc_t, lambda}(x_i{l_i}) from sender
            assert for any l in lock set, pc_f => pc_t join l
            TODO: check the correctness to simplify it to pc_f => l in L
            assert sender => pc, l_i
            TODO: add constaints to limit every arguments' label to be no more trusted than sender/pc
         */
        // TODO: make labels unique

        if (!funcLabels.begin_pc.equals(funcLabels.to_pc)) {
            addLine(assertExp("!" + checkIfLocked(funcLabels.begin_pc, funcLabels.to_pc)) + ";");
            //"ifLocked(" + funcLabels.begin_pc + ")") + ";");
        }
        addLine(assertExp(checkIfTrustSender(funcLabels.begin_pc)) + ";");
        // "ifTrust(" + funcLabels.begin_pc + ", " + "msg.sender)") + ";");

    }

    private String checkIfTrustSender(IfLabel l) {
        if (l instanceof PrimitiveIfLabel) {
            // TODO: error report when missing this entry
            String name = ((PrimitiveIfLabel) l).toString();
            if (name.equals(typecheck.Utils.LABEL_BOTTOM)) {
                return "true"; //TODO
            }
            String addr = labelTable.get(name);
            return "ifTrust(" + addr + ", msg.sender)";
        } else if (l instanceof ComplexIfLabel && ((ComplexIfLabel) l).getOp() == IfOperator.JOIN) {
            return "(" + checkIfTrustSender(((ComplexIfLabel) l).getLeft()) + " || "
                    + checkIfTrustSender(((ComplexIfLabel) l).getRight()) + ")";
        } else {
            // TODO: error report;
            return "NaL";
        }
    }

    private String checkIfLocked(IfLabel l_1, IfLabel l_2) {
        // check if l_1 => l_2 join l
        String name_1, name_2;
        if (!(l_2 instanceof PrimitiveIfLabel)) {
            //TODO: error report
            return null;
        } else {
            name_2 = ((PrimitiveIfLabel) l_2).toString();
        }
        if (l_1 instanceof PrimitiveIfLabel) {
            // TODO: error report when missing this entry
            name_1 = ((PrimitiveIfLabel) l_1).toString();

            if (name_2.equals(typecheck.Utils.LABEL_BOTTOM)
                    || name_1.equals(name_2)) {
                return "false";
            }
            String addr_2 = labelTable.get(name_2);

            if (name_1.equals(typecheck.Utils.LABEL_BOTTOM)) {
                return "ifLocked(" + addr_2 + ")";
            }
            String addr_1 = labelTable.get(name_1);
            return "ifLocked(" + addr_1 + ", " + addr_2 + ")";// + name;
        } else if (l_1 instanceof ComplexIfLabel
                && ((ComplexIfLabel) l_1).getOp() == IfOperator.JOIN) {
            return "(" + checkIfLocked(((ComplexIfLabel) l_1).getLeft(), l_2) + " && "
                    + checkIfLocked(
                    ((ComplexIfLabel) l_1).getRight(), l_2) + ")";
        } else {
            // TODO: error report;
            return "NaL";
        }
    }

    public void enterEndorseBlock(IfLabel l_from, IfLabel l_to) {
        /*
            endorse{l_from -> l_to}:
            assume L is the lock set
            for any l in L, l_from => l_to join l
         */
    }

    public void enterGuard(IfLabel l) {
        /*
            lock(l)
         */
        addLine(assertExp(lock(l)) + ";");
        // "lock(" + l + ")") + ";");
    }

    private String lock(IfLabel l) {
        if (l instanceof PrimitiveIfLabel) {
            // TODO: error report when missing this entry
            String name = ((PrimitiveIfLabel) l).toString();
            if (name.equals(typecheck.Utils.LABEL_BOTTOM)) {
                return "false";//TODO: should make a nop statement
            }
            String addr = labelTable.get(name);
            return "lock(" + addr + ")";
        } else if (l instanceof ComplexIfLabel && ((ComplexIfLabel) l).getOp() == IfOperator.MEET) {
            return "(" + lock(((ComplexIfLabel) l).getLeft()) + " && " + lock(
                    ((ComplexIfLabel) l).getRight())
                    + ")";
        } else {
            // TODO: error report;
            return "NaL";
        }
    }

    private String unlock(IfLabel l) {
        if (l instanceof PrimitiveIfLabel) {
            // TODO: error report when missing this entry
            String name = ((PrimitiveIfLabel) l).toString();
            if (name.equals(typecheck.Utils.LABEL_BOTTOM)) {
                return "false";//TODO: should make a nop statement
            }
            String addr = labelTable.get(name);
            return "unlock(" + addr + ")";
        } else if (l instanceof ComplexIfLabel && ((ComplexIfLabel) l).getOp() == IfOperator.MEET) {
            return "(" + unlock(((ComplexIfLabel) l).getLeft()) + " && " + unlock(
                    ((ComplexIfLabel) l).getRight()) + ")";
        } else {
            // TODO: error report;
            return "NaL";
        }
    }

    public void exitGuard(IfLabel l) {
        /*
            unlock(l);
         */
        addLine(assertExp(unlock(l)) + ";");
        // "unlock(" + l + ")") + ";");
    }

    private String assertExp(String arguments) {
        return "assert(" + arguments + ")";
    }

    public void setDynamicOption(TrustSetting trustSetting) {
        dynamicSystemOption = trustSetting.getDynamicSystemOption();
        labelTable = trustSetting.getLabelTable();
    }
}

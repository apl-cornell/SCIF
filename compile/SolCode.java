package compile;

import ast.*;
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
    public ArrayList<String> code; //each line with no newline char
    public HashMap<String, String> labelTable; // map label names to on-chain addresses

    public SolCode() {
        code = new ArrayList<>();
        labelTable = new HashMap<>();
        labelTable.put("this", "this");
        indentWidth = 4;
        indentLevel = 0;
        unitIndent = "";
        for (int i = 0; i < indentWidth; ++i) unitIndent += " ";
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
        code.add(currentIndent + line);
    }

    public void addVersion(String version) {
        addLine("pragma solidity ^" + version + ";");
    }

    public void addImport(String contractName) {
        addLine("import " + contractName + ";");
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

    public void enterFunctionDef(String name, String args, String returnType, boolean isPublic, boolean isPayable) {
        if (name.equals(Utils.CONSTRUCTOR_NAME))
            addLine("constructor (" + args + ")");
        else
            addLine("function " + name + "(" + args + ")");
        addIndent();
        if (isPublic) addLine(Utils.PUBLIC_DECORATOR);
        else addLine(Utils.PRIVATE_DECORATOR);
        if (isPayable) addLine(Utils.PAYABLE_DECORATOR);
        if (!returnType.isEmpty())
            addLine("returns (" + returnType + ")");
        decIndent();
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
            addLine(assertExp("!" + checkIfLocked(funcLabels.begin_pc)) + ";");
                    //"ifLocked(" + funcLabels.begin_pc + ")") + ";");
        }
        addLine(assertExp(checkIfTrustSender(funcLabels.begin_pc)) + ";");
                // "ifTrust(" + funcLabels.begin_pc + ", " + "msg.sender)") + ";");

    }

    private String checkIfTrustSender(IfLabel l) {
        if (l instanceof PrimitiveIfLabel) {
            // TODO: error report when missing this entry
            String name = ((PrimitiveIfLabel) l).toString();
            if (name.equals(typecheck.Utils.LABEL_BOTTOM)) return "true"; //TODO
            String addr = labelTable.get(name);
            return "ifTrust(" + addr + ", msg.sender)";
        } else if (l instanceof ComplexIfLabel && ((ComplexIfLabel) l).op == IfOperator.JOIN) {
            return "(" + checkIfTrustSender(((ComplexIfLabel) l).left) + " || " + checkIfTrustSender(((ComplexIfLabel) l).right) + ")";
        } else {
            // TODO: error report;
            return "NaL";
        }
    }

    private String checkIfLocked(IfLabel l) {
        if (l instanceof PrimitiveIfLabel) {
            // TODO: error report when missing this entry
            String name = ((PrimitiveIfLabel) l).toString();

            if (name.equals(typecheck.Utils.LABEL_BOTTOM)) return "false"; //TODO
            String addr = labelTable.get(name);
            return "ifLocked(" + addr + ")";// + name;
        } else if (l instanceof ComplexIfLabel && ((ComplexIfLabel) l).op == IfOperator.JOIN) {
            return "(" + checkIfLocked(((ComplexIfLabel) l).left) + " && " + checkIfLocked(((ComplexIfLabel) l).right) + ")";
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
            if (name.equals(typecheck.Utils.LABEL_BOTTOM)) return "false";//TODO: should make a nop statement
            String addr = labelTable.get(name);
            return "lock(" + addr + ")";
        } else if (l instanceof ComplexIfLabel && ((ComplexIfLabel) l).op == IfOperator.MEET) {
            return "(" + lock(((ComplexIfLabel) l).left) + " && " + lock(((ComplexIfLabel) l).right) + ")";
        } else {
            // TODO: error report;
            return "NaL";
        }
    }

    private String unlock(IfLabel l) {
        if (l instanceof PrimitiveIfLabel) {
            // TODO: error report when missing this entry
            String name = ((PrimitiveIfLabel) l).toString();
            if (name.equals(typecheck.Utils.LABEL_BOTTOM)) return "false";//TODO: should make a nop statement
            String addr = labelTable.get(name);
            return "unlock(" + addr + ")";
        } else if (l instanceof ComplexIfLabel && ((ComplexIfLabel) l).op == IfOperator.MEET) {
            return "(" + unlock(((ComplexIfLabel) l).left) + " && " + unlock(((ComplexIfLabel) l).right) + ")";
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
}

package compile;

import ast.IfLabel;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.util.ArrayList;

public class SolCode {
    int indentWidth;
    int indentLevel;
    String unitIndent;
    String currentIndent;
    public ArrayList<String> code; //each line with no newline char

    public SolCode() {
        code = new ArrayList<>();
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
        System.err.println("ADDING: " + currentIndent + line);
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
        addLine("contract " + contractName + " {");
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

    public void enterFuncCheck(String funcName) {
        /*
            f{pc}(x_i{l_i}) from sender
            assert sender => pc, l_i
         */
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
    }

    public void exitGuard() {
        /*
            unlock(l);
         */
    }
}

package compile;

import ast.*;
import compile.ast.Argument;
import compile.ast.Call;
import compile.ast.Expression;
import compile.ast.Literal;
import compile.ast.PrimitiveType;
import compile.ast.SingleVar;
import compile.ast.SolNode;
import compile.ast.Statement;
import compile.ast.TupleType;
import compile.ast.Type;
import compile.ast.VarDec;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

public class Utils {
    public static final String DEFAULT_SOLITIDY_VERSION = "0.8.3";
    public static final String PUBLIC_DECORATOR = "public";
    public static final String PAYABLE_DECORATOR = "payable";
    public static final String PRIVATE_DECORATOR = "private";
    public static final String CONSTRUCTOR_NAME = "constructor";
    public static final String RESULT_PATH_NAME = "resultPath";
    public static final String EXCEPTION_RECORDER_NAME = "lastThrownException";
    public static final String EXCEPTION_RECORDER_DATA_NAME = "lastThrownExceptionData";

    public static final PrimitiveType PRIMITIVE_TYPE_UINT = new PrimitiveType("uint");
    public static final PrimitiveType PRIMITIVE_TYPE_BYTES = new PrimitiveType("bytes");
    public static final TupleType UNIVERSAL_RETURN_TYPE = new TupleType(List.of(PRIMITIVE_TYPE_UINT, PRIMITIVE_TYPE_BYTES));
    public static final String RESULT_VAR_NAME = "result";
    public static final String SOL_BOOL_EQUAL = "==";
    public static final String RETURNCODE_RETURN = "10000";
    public static final String RETURNCODE_NORMAL = "0";
    public static final String SOL_BOOL_NONEQUAL = "!=";
    public static final PrimitiveType PRIMITIVE_TYPE_BOOL = new PrimitiveType("bool");
    public static final SingleVar THIS_ADDRESS = new SingleVar("address(this)");
    public static final String SOL_TRUE = "true";
    public static final String TRUSTS_CALL = "trusts";
    public static final String LOCK_CALL = "acquireLock";
    public static final String UNLOCK_CALL = "releaseLock";
    public static final String NATIVE_DECORATOR = "native";
    public static final String BYPASSLOCK_CALL = "bypassLocks";
    public static final String SOL_PUBLIC_DECORATOR = "public";
    public static final String SOL_PRIVATE_DECORATOR = "internal";
    public static final String RETURNCODE_FAILURE = "9999";

    public static String toBinOp(BinaryOperator op) {
        if (op == BinaryOperator.Add)
            return "+";
        else if (op == BinaryOperator.BitAnd)
            return "&";
        else if (op == BinaryOperator.BitOr)
            return "|";
        else if (op == BinaryOperator.BitXor)
            return "^";
        else if (op == BinaryOperator.Div)
            return "/";
        else if (op == BinaryOperator.Lshift)
            return "<<";
        else if (op == BinaryOperator.Mod)
            return "%";
        else if (op == BinaryOperator.Mult)
            return "*";
        else if (op == BinaryOperator.Pow)
            return "**";
        else if (op == BinaryOperator.Rshift)
            return ">>";
        else if (op == BinaryOperator.Sub)
            return "-";
        else
            return "unknown";
    }

    public static String toBoolOp(BoolOperator op) {
        if (op == BoolOperator.And)
            return "&&";
        else if (op == BoolOperator.Or)
            return "||";
        else
            return "unknown";
    }

    public static String toCompareOp(CompareOperator op) {
        if (op == CompareOperator.Eq)
            return "==";
        else if (op == CompareOperator.NotEq)
            return "!=";
        else if (op == CompareOperator.Lt)
            return "<";
        else if (op == CompareOperator.LtE)
            return "<=";
        else if (op == CompareOperator.Gt)
            return ">";
        else if (op == CompareOperator.GtE)
            return ">=";
        else
            return "NOTSUPPORTED";
    }

    public static String toConstant(Constant value) {
        if (value == Constant.FALSE)
            return "false";
        else if (value == Constant.TRUE)
            return "true";
        else if (value == Constant.NONE)
            return "none";
        else
            return "unknown";
    }

    public static String toUnaryOp(UnaryOperator op) {
        if (op == UnaryOperator.Invert)
            return "~";
        else if (op == UnaryOperator.Not)
            return "!";
        else if (op == UnaryOperator.UAdd)
            return "+";
        else if (op == UnaryOperator.USub)
            return "-";
        else
            return "unknown";
    }


    public static String version(String version) {
        return "pragma solidity >=" + version + ";";
    }

    public static String codeImport(String contractName) {
        return "import \"./" + contractName + ".sol\";";
    }

    public static void addLine(List<String> result, String line, int indentLevel) {
        result.add(addIndent(line, indentLevel));
    }

    public static String addIndent(String line, int indentLevel) {
        return String.join("", Collections.nCopies(indentLevel * 2, " ")) + line;
    }

    public static String decodeCall(String data, String types) {
        return "abi.decode(" + data + ", " + types + ")";
    }
    public static String encodeCall(String values) {
        return "abi.encode(" + values + ")";
    }

    public static String tupleSol(List<Expression> targets) {
        assert targets.size() > 0;
        return targets.size() == 1 ? targets.get(0).toSolCode() : targets.stream().map(
                Expression::toSolCode).collect(
                Collectors.joining(", "));
    }

    public static String argsListSol(List<Argument> arguments) {
        // T1 name1, T2 name2, ...
        return arguments.stream().map(a -> a.type().solCode() + ", " + a.name()).collect(
                Collectors.joining(", "));
    }


    public static void writeToFile(SolNode node, File outputFile) {
        try {
            BufferedWriter writer = new BufferedWriter(new FileWriter(outputFile));
            for (String line: node.toSolCode(0)) {
                writer.write(line + "\n");
            }
            writer.close();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    public static void printCode(SolNode node) {
        System.err.println(String.join("\n", node.toSolCode(0)));
    }

    public static Expression translateBuiltInFunc(Call callExp) {
        if (callExp.funcName().equals("send")) {
            return new Call("assert", List.of(new Call("payable(" + callExp.arg(0).toSolCode() + ").send", List.of(callExp.arg(1)))));
        } else {
            return callExp;
        }
    }

    public static void addBuiltInVars(boolean isPublic, List<Statement> statements, CompileEnv code) {
        String varName;
        Type varType;
        // address sender = msg.sender
        varType = new PrimitiveType("address");
        varName = typecheck.Utils.LABEL_SENDER;
        VarDec senderDec = new VarDec(varType, varName, new Literal("msg.sender"));
        code.addLocalVar(varName, varType);
        statements.add(senderDec);
        // uint value = msg.value
        if (isPublic) {
            varType = new PrimitiveType("uint");
            varName = typecheck.Utils.LABEL_PAYVALUE;
            VarDec valueDec = new VarDec(varType, varName, new Literal("msg.value"));
            code.addLocalVar(varName, varType);
            statements.add(valueDec);
        }
    }
}

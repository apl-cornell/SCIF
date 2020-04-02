package compile;

import ast.*;

public class Utils {
    public static final String SOLITIDY_VERSION = "0.6.4";
    public static final String PUBLIC_DECORATOR = "public";
    public static final String PAYABLE_DECORATOR = "payable";
    public static final String PRIVATE_DECORATOR = "private";

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
}

package compile.ast;

import static compile.Utils.PAYABLE_DECORATOR;
import static compile.Utils.addLine;

import ast.Arg;
import compile.Utils;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

public class FunctionSig implements SolNode {
    String name;
    List<Argument> args;
    Type returnType;
    boolean isExternal;
    boolean isPayable;

    public FunctionSig(String name, List<Argument> args, Type returnType, boolean isExternal, boolean isPayable) {
        this.name = name;
        this.args = args;
        this.returnType = returnType;
        this.isExternal = isExternal;
        this.isPayable = isPayable;
    }

    @Override
    public List<String> toSolCode(int indentLevel) {
        List<String> result = new ArrayList<>();
        addLine(result,
                "function " +
                        name + "(" +
                        String.join(", ",
                                args.stream().map(
                                        arg -> arg.type.solCode() + " " + arg.name
                                ).collect(Collectors.toList())) +
                        ")",
                indentLevel);
        addLine(result, isExternal ? Utils.SOL_EXTERNAL_DECORATOR : Utils.SOL_PRIVATE_DECORATOR, indentLevel + 1);
        if (isExternal) {
            addLine(result, PAYABLE_DECORATOR, indentLevel + 1);
        }
        if (!(returnType instanceof PrimitiveType && ((PrimitiveType) returnType).name.equals("void"))) {
            addLine(result,
                    "returns " + (returnType instanceof TupleType ? returnType.solCode(true) : "(" + returnType.solCode(true) + " " + Utils.RESULT_VAR_NAME + ")"),
                    indentLevel + 1);
        }
        addLine(result, ";", indentLevel + 1);
        return result;
    }

    public String funcName() {
        return name;
    }

    public List<String> argNames() {
        return args.stream().map(arg -> arg.name).collect(Collectors.toList());
    }

    public String signature() {
        return funcName() + "(" + args.stream().map(arg -> arg.type.solCode()).collect(
                Collectors.joining(","))
                + ")";
    }
}

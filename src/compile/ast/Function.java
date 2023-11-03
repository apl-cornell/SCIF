package compile.ast;

import static compile.Utils.PAYABLE_DECORATOR;
import static compile.Utils.addLine;

import compile.Utils;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

public class Function extends FunctionSig {
    List<Statement> body;

    public Function(String name, List<Argument> args, Type returnType, boolean isExternal, boolean isPayable, List<Statement> body) {
        super(name, args, returnType, isExternal, isPayable);
        this.body = body;
        // addBuiltInVarDefs();
    }
//
//    public static List<Statement> builtInVarDefs(boolean exceptionFree, Type returnType) {
//        List<Statement> result = new ArrayList<>();
//        if (!exceptionFree) {
//            result.add(new VarDec(Utils.PRIMITIVE_TYPE_UINT, Utils.RESULT_PATH_NAME, new Literal("0")));
//            result.add(
//                    new VarDec(Utils.PRIMITIVE_TYPE_UINT, Utils.EXCEPTION_RECORDER_NAME, new Literal("0")));
//            result.add(new VarDec(Utils.PRIMITIVE_TYPE_BYTES, Utils.EXCEPTION_RECORDER_DATA_NAME));
//        }
//        return result;
//    }

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
        addLine(result, isExternal ? Utils.SOL_PUBLIC_DECORATOR : Utils.SOL_PRIVATE_DECORATOR, indentLevel + 1);
        if (isExternal) {
            addLine(result, PAYABLE_DECORATOR, indentLevel + 1);
        }
        if (!(returnType instanceof PrimitiveType && ((PrimitiveType) returnType).name.equals("void"))) {
            addLine(result,
                    "returns " + (returnType instanceof TupleType ? returnType.solCode(true) : "(" + returnType.solCode(true) + " " + Utils.RESULT_VAR_NAME + ")"),
                    indentLevel + 1);
        }
        addLine(result, "{", indentLevel);

        for (Statement s: body) {
            result.addAll(s.toSolCode(indentLevel + 1));
        }

        addLine(result, "}", indentLevel);
        return result;
    }
}

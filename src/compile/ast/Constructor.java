package compile.ast;

import static compile.Utils.addLine;

import compile.Utils;
import java.util.ArrayList;
import java.util.List;

public class Constructor extends Function {
    // List<Argument> arguments;
    // List<Statement> body;

    public Constructor(List<Argument> arguments, List<Statement> body) {
        super("constructor", arguments, null, true, false, body);
    }

    @Override
    public List<String> toSolCode(int indentLevel) {
        List<String> result = new ArrayList<>();
        addLine(result, "constructor(" +
                Utils.argsListSol(args, true)
                + ") {",indentLevel);
        for (Statement s: body) {
            result.addAll(s.toSolCode(indentLevel + 1));
        }
        addLine(result, "}", indentLevel);
        return result;
    }
}

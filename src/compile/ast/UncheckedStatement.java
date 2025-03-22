package compile.ast;

import static compile.Utils.addLine;

import java.util.ArrayList;
import java.util.List;

public class UncheckedStatement implements Statement {
    List<Statement> body;

    public UncheckedStatement(List<Statement> body) {
        this.body = body;
    }

    @Override
    public List<String> toSolCode(int indentLevel) {
        List<String> result = new ArrayList<>();
        addLine(result, "unchecked {", indentLevel);
        for (Statement s: body) {
            result.addAll(s.toSolCode(indentLevel + 1));
        }
        addLine(result, "}", indentLevel);
        return result;
    }
}

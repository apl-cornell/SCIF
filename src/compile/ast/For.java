package compile.ast;

import static compile.Utils.addLine;

import java.util.ArrayList;
import java.util.List;

public class For implements Statement {
    String init;
    Expression cond;
    String iter;
    List<Statement> body;

    public For(String init, Expression cond, String iter, List<Statement> body) {
        this.init = init;
        this.cond = cond;
        this.iter = iter;
        this.body = body;
    }

    @Override
    public List<String> toSolCode(int indentLevel) {
        List<String> result = new ArrayList<>();
        addLine(result, "for (" + init + "; " + cond.toSolCode() + "; " + iter + ") {", indentLevel);
        for (Statement s: body) {
            result.addAll(s.toSolCode(indentLevel + 1));
        }
        addLine(result, "}", indentLevel);
        return result;
    }
}

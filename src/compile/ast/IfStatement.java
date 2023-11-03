package compile.ast;

import static compile.Utils.addLine;

import java.util.ArrayList;
import java.util.List;

public class IfStatement implements Statement {
    Expression condition;
    List<Statement> ifBody;
    List<Statement> elseBody;

    public IfStatement(Expression condition, List<Statement> ifBody, List<Statement> elseBody) {
        this.condition = condition;
        this.ifBody = ifBody;
        this.elseBody = elseBody;
    }

    public IfStatement(IfStatement ifpart, List<Statement> elseBody) {
        this.condition = ifpart.condition;
        this.ifBody = ifpart.ifBody;
        this.elseBody = elseBody;
    }

    public IfStatement(Expression condition, List<Statement> ifBody) {
        this.condition = condition;
        this.ifBody = ifBody;
    }

    @Override
    public List<String> toSolCode(int indentLevel) {
        List<String> result = new ArrayList<>();
        addLine(result, "if (" + condition.toSolCode() + ") {", indentLevel);
        for (Statement s: ifBody) {
            result.addAll(s.toSolCode(indentLevel + 1));
        }
        if (elseBody != null && elseBody.size() > 0) {
            addLine(result, "} else {", indentLevel);
            for (Statement s: elseBody) {
                result.addAll(s.toSolCode(indentLevel + 1));
            }
        }
        addLine(result, "}", indentLevel);
        return result;
    }
}

package compile.ast;

import compile.Utils;
import java.util.ArrayList;
import java.util.List;

public class Assert implements Statement {
    Expression condition;

    public Assert(Expression condition) {
        this.condition = condition;
    }

    @Override
    public List<String> toSolCode(int indentLevel) {
        List<String> result = new ArrayList<>();
        Utils.addLine(result, "assert(" + condition.toSolCode() + ");", indentLevel);
        return result;
    }
}

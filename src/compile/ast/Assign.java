package compile.ast;

import static compile.Utils.addLine;

import compile.Utils;
import java.util.ArrayList;
import java.util.List;

public class Assign implements Statement {
    List<Expression> targets;
    Expression value;

    public Assign(Expression target, Expression value) {
        this.targets = List.of(target);
        this.value = value;
    }

    public Assign(List<Expression> targets, Expression value) {
        this.targets = targets;
        this.value = value;
    }

    @Override
    public List<String> toSolCode(int indentLevel) {
        List<String> result = new ArrayList<>();

        addLine(result,
                "(" + Utils.tupleSol(targets) + ") = " + value.toSolCode() + ";",
                indentLevel);
        return result;
    }
}

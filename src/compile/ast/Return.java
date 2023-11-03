package compile.ast;

import static compile.Utils.addLine;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

public class Return implements Statement {
    List<Expression> values;

    public Return(List<Expression> values) {
        this.values = values;
    }

    public Return(Expression value) {
        this.values = List.of(value);
    }

    public Return() {
    }

    @Override
    public List<String> toSolCode(int indentLevel) {
        List<String> result = new ArrayList<>();
        if (values == null || values.size() == 0) {
            addLine(result, "return;", indentLevel);
        } else if (values.size() == 1) {
            addLine(result, "return " + values.get(0).toSolCode() + ";", indentLevel);
        } else {
            addLine(result, "return (" + String.join(", ", values.stream().map(value -> value.toSolCode()).collect(
                    Collectors.toList()))+ ");", indentLevel);
        }
        return result;
    }
}

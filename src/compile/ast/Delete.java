package compile.ast;

import static compile.Utils.addLine;

import compile.Utils;
import java.util.ArrayList;
import java.util.List;

public class Delete implements Statement {

    Expression target;

    public Delete(Expression target) {
        this.target = target;
    }

    @Override
    public List<String> toSolCode(int indentLevel) {
        List<String> result = new ArrayList<>();

        addLine(result,
                "delete " + target.toSolCode() + ";",
                indentLevel);
        return result;
    }
}

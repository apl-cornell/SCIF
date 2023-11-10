package compile.ast;

import static compile.Utils.addLine;

import java.util.ArrayList;
import java.util.List;

public class Revert implements Statement {
    Expression msg;

    public Revert(Expression msg) {
        this.msg = msg;
    }
    public Revert() {
    }

    @Override
    public List<String> toSolCode(int indentLevel) {
        List<String> result = new ArrayList<>();
        addLine(result, "revert(\"" + (msg != null ? msg.toSolCode() : "") +"\");", indentLevel);
        return result;
    }
}

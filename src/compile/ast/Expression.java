package compile.ast;

import static compile.Utils.addLine;

import java.util.ArrayList;
import java.util.List;

public abstract class Expression implements Statement {

    public abstract String toSolCode();

    @Override
    public List<String> toSolCode(int indentLevel) {
        List<String> result = new ArrayList<>();
        addLine(result, toSolCode() + ";", indentLevel);
        return result;
    }
}

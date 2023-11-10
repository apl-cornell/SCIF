package compile.ast;

import java.util.ArrayList;
import java.util.List;

public class Pass extends Expression {

    @Override
    public String toSolCode() {
        assert false: "pass should not appear as an expression";
        return null;
    }

    @Override
    public List<String> toSolCode(int indentLevel) {
        return new ArrayList<>();
    }
}

package compile.ast;

import java.util.List;

public class SingleVar extends Expression {
    String name;
    public SingleVar(String name) {
        this.name = name;
    }

    public String name() {
        return name;
    }

    @Override
    public String toSolCode() {
        return name;
    }
}

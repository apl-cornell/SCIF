package ast;

import compile.CompileEnv;
import compile.ast.Statement;
import java.util.ArrayList;
import java.util.List;

public class Str extends Literal {
    String value;
    public Str(String x) {
        value = x;
    }

//    @Override
//    public String toSolCode() {
//        return value;
//    }

    @Override
    public compile.ast.Expression solidityCodeGen(List<Statement> result, CompileEnv code) {
        return new compile.ast.Literal("\"" + value + "\"");
    }

    @Override
    public boolean typeMatch(Expression expression) {
        return expression instanceof Str &&
                value.equals(((Str) expression).value);
    }
    @Override
    public List<Node> children() {
        List<Node> rtn = new ArrayList<>();
        return rtn;
    }
}

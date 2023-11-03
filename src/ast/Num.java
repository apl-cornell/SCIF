package ast;

import compile.CompileEnv;
import compile.ast.Statement;
import java.util.ArrayList;
import java.util.List;

public class Num<T extends Number> extends Literal {
    T value;
    public Num(T x) {
        value = x;
    }

//    @Override
//    public String toSolCode() {
//        return value.toString();
//    }

    @Override
    public compile.ast.Expression solidityCodeGen(List<Statement> result, CompileEnv code) {
        return new compile.ast.Literal(value.toString());
    }

    @Override
    public boolean typeMatch(Expression expression) {
        return expression instanceof Num &&
                value.equals(((Num) expression).value);
    }
    @Override
    public List<Node> children() {
        List<Node> rtn = new ArrayList<>();
        return rtn;
    }
}

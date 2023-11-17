package compile.ast;

import java.util.List;
import java.util.stream.Collectors;

public class New extends Expression {
    String contractName;
    List<Expression> argExps;

    public New(String contractName, List<Expression> argExps) {
        this.contractName = contractName;
        this.argExps = argExps;
    }


    @Override
    public String toSolCode() {
        return "new " + contractName + "(" +
                argExps.stream().map(Expression::toSolCode).collect(
                        Collectors.joining(", ")) +
                ")";
    }
}

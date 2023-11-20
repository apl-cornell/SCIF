package compile.ast;

import java.util.List;
import java.util.stream.Collectors;

public class New extends Expression {
    String contractName;
    List<Expression> argExps;
    CallSpec callSpec;

    public New(String contractName, List<Expression> argExps, CallSpec callSpec) {
        this.contractName = contractName;
        this.argExps = argExps;
        this.callSpec = callSpec;
    }


    @Override
    public String toSolCode() {
        return "new " + contractName +
                (callSpec != null ? callSpec.toSolCode() : "") +
                "(" +
                argExps.stream().map(Expression::toSolCode).collect(
                        Collectors.joining(", ")) +
                ")";
    }
}

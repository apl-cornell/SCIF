package compile.ast;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

public class Call extends Expression {
    String funcName;
    List<Expression> argValues;
    CallSpec callSpec;

    public Call(String funcName, List<Expression> argValues) {
        this.funcName = funcName;
        this.argValues = argValues;
    }
    public Call(String funcName, List<Expression> argValues, CallSpec callSpec) {
        this.funcName = funcName;
        this.argValues = argValues;
        this.callSpec = callSpec;
    }

    public Call(String funcName) {
        this.funcName = funcName;
        argValues = new ArrayList<>();
    }

    @Override
    public String toSolCode() {
        return funcName +
                (callSpec != null ? callSpec.toSolCode() : "") +
                "(" +
                    String.join(", ", argValues.stream().map(value -> value.toSolCode()).collect(
                        Collectors.toList())) +
                ")";
    }

    public Object funcName() {
        return funcName;
    }

    public Expression arg(int i) {
        return argValues.get(i);
    }
}

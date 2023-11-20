package compile.ast;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public class CallSpec extends Expression {
    Map<String, Expression> specs;

    public CallSpec(Map<String, Expression> specs) {
        this.specs = specs;
    }

    @Override
    public String toSolCode() {
        return "{" +
                specs.entrySet().stream().map(entry -> entry.getKey() + ": " + entry.getValue().toSolCode()).collect(
                        Collectors.joining(", ")) +
                "}";
    }
}

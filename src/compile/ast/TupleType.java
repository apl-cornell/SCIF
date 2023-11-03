package compile.ast;

import java.util.List;
import java.util.stream.Collectors;

public class TupleType implements Type {
    List<Type> tuple;

    public TupleType(List<Type> tuple) {
        this.tuple = tuple;
    }

    @Override
    public String solCode() {
        return "(" +
                String.join(", ", tuple.stream().map(t -> t.solCode()).collect(Collectors.toList())) +
                ")";
    }

    @Override
    public String solCode(boolean isLocal) {
        return "(" +
                String.join(", ", tuple.stream().map(t -> t.solCode(isLocal)).collect(Collectors.toList())) +
                ")";

    }
}

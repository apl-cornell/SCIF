package typecheck;

import ast.IfOperator;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class ComplexLabel extends Label {
    private final Label left, right;
    private final IfOperator op;

    @Override
    public List<String> principalLeaves() {
        List<String> leaves = new ArrayList<>(left.principalLeaves());
        leaves.addAll(right.principalLeaves());
        return leaves;
    }

    public ComplexLabel(Label left, IfOperator op, Label right, CodeLocation location) {
        super(location);
        this.left = left;
        this.right = right;
        this.op = op;
    }

    @Override
    public String toSHErrLocFmt() {
        String l = left.toSHErrLocFmt();
        String r = right.toSHErrLocFmt();
        return switch (op) {
            case JOIN -> "(" + l + " ⊔ " + r + ")";
            case MEET -> "(" + l + " ⊓ " + r + ")";
        };
    }

    @Override
    public String toSHErrLocFmt(String origin, String substitution) {
        String l = left.toSHErrLocFmt(origin, substitution);
        String r = right.toSHErrLocFmt(origin, substitution);
        return switch (op) {
            case JOIN -> "(" + l + " ⊔ " + r + ")";
            case MEET -> "(" + l + " ⊓ " + r + ")";
        };
    }

    @Override
    public String toSHErrLocFmt(Map<String, String> mapping) {
        String l = left.toSHErrLocFmt(mapping);
        String r = right.toSHErrLocFmt(mapping);
        return switch (op) {
            case JOIN -> "(" + l + " ⊔ " + r + ")";
            case MEET -> "(" + l + " ⊓ " + r + ")";
        };
    }

}

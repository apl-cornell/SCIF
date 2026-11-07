package typecheck.sherrlocUtils;

import ast.CompareOperator;
import sherrloc.diagnostic.SherrlocDiagnoser;

import java.util.List;
import sherrloc.constraint.ast.Element;
import sherrloc.constraint.ast.Position;

// a <= b means a flows to b
// that is, a >= b in terms of integrity level
// in case of regular typechecking, a <= b means a is supertype of b
public class Inequality {
    String lhs, rhs;
    Relation relation;
    public Inequality(String lhs, Relation relation, String rhs) {
        assert !lhs.startsWith("null");
        assert !rhs.startsWith("null");
        assert !lhs.equals("");
        assert !rhs.equals("");
        this.lhs = lhs;
        this.rhs = rhs;
        this.relation = relation;
    }
    public Inequality(String lhs, CompareOperator co, String rhs) {
        assert !lhs.startsWith("null") : lhs;
        assert !rhs.startsWith("null") : rhs;
        assert !lhs.equals("");
        assert !rhs.equals("");
        this.lhs = lhs;
        this.rhs = rhs;
        if (co == CompareOperator.Eq)
            this.relation = Relation.EQ;
        else if (co == CompareOperator.GtE)
            this.relation = Relation.GEQ;
        else if (co == CompareOperator.LtE)
            this.relation = Relation.LEQ;
    }

    public Inequality(String lhs, String rhs) {
        assert !lhs.startsWith("null");
        assert !rhs.startsWith("null");
        this.lhs = lhs;
        this.rhs = rhs;
        this.relation = Relation.LEQ;
    }

    public String toSherrlocFmt() {
        if (relation == Relation.EQ) {
            return lhs + " == " + rhs;
        } else if (relation == Relation.LEQ) {
            return lhs + " <= " + rhs;
        } else if (relation == Relation.GEQ) {
            return lhs + " >= " + rhs;
        } else {
            //TODO: error   
            return "";
        }
    }

    public sherrloc.constraint.ast.Inequality toSherrlocInequality(SherrlocDiagnoser diagnoser) {
        var e = toElement(lhs, diagnoser);
        var o = toElement(rhs, diagnoser);

        if (relation == Relation.EQ) {
            return diagnoser.createEqualityConstraint(e, o);
        } else if (relation == Relation.LEQ) {
            return diagnoser.createLessThanConstraint(e, o);
        } else {
            return diagnoser.createGreaterThanConstraint(e, o);
        }
    }

    /**
     * The element denoted by {@code label}, a name or a join or meet written as
     * {@code (l ⊔ r)} or {@code (l ⊓ r)}. Each name resolves to the constructor or
     * variable that {@code diagnoser} knows by that name.
     */
    private static Element toElement(String label, SherrlocDiagnoser diagnoser) {
        // A composite string handed to the diagnoser whole would be parsed with
        // empty tables, turning every name inside it into a fresh variable.
        if (label.startsWith("(") && label.endsWith(")")) {
            String inner = label.substring(1, label.length() - 1);
            int depth = 0;
            for (int i = 0; i < inner.length(); i++) {
                char c = inner.charAt(i);
                if (c == '(') {
                    depth++;
                } else if (c == ')') {
                    depth--;
                    if (depth < 0) {
                        break; // the outer parentheses do not enclose the whole label
                    }
                } else if (depth == 0 && (c == '⊔' || c == '⊓')
                        && i > 0 && inner.charAt(i - 1) == ' ' && i + 1 < inner.length()
                        && inner.charAt(i + 1) == ' ') {
                    List<Element> operands = List.of(
                            toElement(inner.substring(0, i - 1), diagnoser),
                            toElement(inner.substring(i + 2), diagnoser));
                    return c == '⊔' ? diagnoser.createJoinListElement(operands)
                            : diagnoser.createMeetListElement(operands);
                }
            }
            if (depth == 0) {
                return toElement(inner, diagnoser);
            }
        }
        return diagnoser.createElement(label, Position.EmptyPosition());
    }
}

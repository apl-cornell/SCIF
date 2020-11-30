package sherrlocUtils;

import ast.Compare;
import ast.CompareOperator;

// a <= b means a flows to b
// that is, a >= b in terms of integrity level
// in case of regular typechecking, a <= b means a is supertype of b
public class Inequality {
    String lhs, rhs;
    Relation relation;
    public Inequality(String lhs, Relation relation, String rhs) {
        if (lhs.equals("") || rhs.equals("")) {
            // System.err.println(lhs + " | " + rhs);
            int a = 1/0;
        }
        this.lhs = lhs;
        this.rhs = rhs;
        this.relation = relation;
    }
    public Inequality(String lhs, CompareOperator co, String rhs) {
        if (lhs.equals("") || rhs.equals("")) {
            // System.err.println(lhs + " | " + rhs);
            int a = 1/0;
        }
        this.lhs = lhs;
        this.rhs = rhs;
        if (co == CompareOperator.Eq)
            this.relation = Relation.EQ;
        else if (co == CompareOperator.GtE)
            this.relation = Relation.REQ;
        else if (co == CompareOperator.LtE)
            this.relation = Relation.LEQ;
    }

    public Inequality(String lhs, String rhs) {
        if (lhs.equals("") || rhs.equals("")) {
            // System.err.println(lhs + " | " + rhs);
            int a = 1/0;
        }
        this.lhs = lhs;
        this.rhs = rhs;
        this.relation = Relation.LEQ;
    }

    public String toSherrlocFmt() {
        if (relation == Relation.EQ) {
            return lhs + " == " + rhs;
        } else if (relation == Relation.LEQ) {
            return lhs + " <= " + rhs;
        } else if (relation == Relation.REQ) {
            return lhs + " >= " + rhs;
        } else {
            //TODO: error
            return "";
        }
    }
}

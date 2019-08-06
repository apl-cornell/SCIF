package typecheck;

import ast.IfLabel;

public class IfConstraint {
    public String op;
    public String left, right;
    public CodeLocation location;
    public IfConstraint() {
        op = null;
    }
    public IfConstraint(String op, String left, String right, CodeLocation location) {
        this.op = op;
        this.left = left;
        this.right = right;
        this.location = location;
    }
    public IfConstraint(String op, IfLabel left, IfLabel right, CodeLocation location) {
        this.op = op;
        this.left = left.toSherrlocFmt();
        this.right = right.toSherrlocFmt();
        this.location = location;
    }
    public String toSherrlocFmt() {
        if (op == null) {
            return "";
        }
        String rnt = "";
        String l = left;//.toSherrlocFmt();
        String r = right;//.toSherrlocFmt();
        rnt = l + " " + op + " " + r + "; ";
        if (location.valid())
            rnt += "[" + location.toSherrlocFmt() + "]\n";
        else
            rnt += "\n";
        return rnt;
    }
}

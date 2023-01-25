package typecheck;

import ast.IfLabel;

public class IfConstraint {
    public String op;
    public String left, right;
    public CodeLocation codeLocation;
    public String contractName;
    public IfConstraint() {
        op = null;
    }
    public IfConstraint(String op, String left, String right, CodeLocation location, String contractName) {
        this.op = op;
        this.left = left;
        this.right = right;
        this.left = checkThis(this.left, contractName);
        this.right = checkThis(this.right, contractName);

        this.codeLocation = location;
        this.contractName = contractName;
    }
//    public IfConstraint(String op, IfLabel left, IfLabel right, CodeLocation location, String contractName) {
//        this.op = op;
//        this.left = left.toSHErrLocFmt(contractName);
//        this.right = right.toSHErrLocFmt(contractName);
//        this.left = checkThis(this.left, contractName);
//        this.right = checkThis(this.right, contractName);
//
//        this.codeLocation = location;
//        this.contractName = contractName;
//    }
    public String toSherrlocFmt(boolean withPosition) {
        if (op == null) {
            return "";
        }
        String rnt = "";
        String l = left;//.toSHErrLocFmt();
        String r = right;//.toSHErrLocFmt();
        rnt = l + " " + op + " " + r + "; ";
        if (withPosition && codeLocation.valid())
            rnt += "[" + codeLocation.toSherrlocFmt() + "]\n";
        else
            rnt += "\n";
        return rnt;
    }

    private String checkThis(String label, String contractName) {
        if (label.equals(Utils.LABEL_THIS)) {
            return contractName + "." + label;
        }
        return label;
    }
}

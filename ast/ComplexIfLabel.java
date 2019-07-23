package ast;

import utils.CodeLocation;

import java.util.HashSet;

public class ComplexIfLabel extends IfLabel {
    IfOperator op;
    IfLabel left, right;
    public ComplexIfLabel(IfLabel left, IfOperator op, IfLabel right) {
        this.left = left;
        this.op = op;
        this.right = right;
    }
    @Override
    public String toSherrlocFmt() {
        String l = left.toSherrlocFmt();
        String r = right.toSherrlocFmt();
        String rnt = "";
        switch (op) {
            case JOIN:
                rnt = "(" + l + " ⊔ " + r + ")";
                break;
            case MEET:
                rnt = "(" + l + " ⊓ " + r + ")";
                break;

        }
        return rnt;
    }
    public void findPrincipal(HashSet<String> principalSet) {
        left.findPrincipal(principalSet);
        right.findPrincipal(principalSet);
    }
}

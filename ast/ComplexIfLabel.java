package ast;

import java.util.HashSet;

public class ComplexIfLabel extends IfLabel {
    public IfOperator op;
    public IfLabel left, right;
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
    public String toSherrlocFmt(String namespace) {
        String l = left.toSherrlocFmt(namespace);
        String r = right.toSherrlocFmt(namespace);
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

    public String toSherrlocFmt(String k, String v) {
        String l = left.toSherrlocFmt(k, v);
        String r = right.toSherrlocFmt(k, v);
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

    public String toSherrlocFmtApply(HashSet<String> strSet, int no) {
        String l = left.toSherrlocFmtApply(strSet, no);
        String r = right.toSherrlocFmtApply(strSet, no);
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

    @Override
    public void findPrincipal(HashSet<String> principalSet, String getRidOf) {
        left.findPrincipal(principalSet, getRidOf);
        right.findPrincipal(principalSet, getRidOf);
    }

    @Override
    public void findPrincipal(HashSet<String> principalSet) {
        left.findPrincipal(principalSet);
        right.findPrincipal(principalSet);
    }

    @Override
    public boolean typeMatch(IfLabel begin_pc) {
        if (!(begin_pc instanceof ComplexIfLabel))
            return false;

        ComplexIfLabel cil = (ComplexIfLabel) begin_pc;
        return op == cil.op && left.typeMatch(cil.left) && right.typeMatch(cil.right);
    }

    public void replace(String k, String v) {
        left.replace(k, v);
        right.replace(k, v);
    }

    @Override
    public boolean typeMatch(Expression expression) {
        return expression instanceof ComplexIfLabel &&
                typeMatch((IfLabel) expression);
    }
}

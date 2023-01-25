package ast;

import typecheck.ExpOutcome;
import typecheck.NTCEnv;
import typecheck.ScopeContext;
import typecheck.VisitEnv;

import java.util.HashSet;

public class ComplexIfLabel extends IfLabel {

    private final IfOperator op;
    private final IfLabel left, right;

    public IfOperator getOp() {
        return op;
    }

    public IfLabel getLeft() {
        return left;
    }

    public IfLabel getRight() {
        return right;
    }

    public ComplexIfLabel(IfLabel left, IfOperator op, IfLabel right) {
        this.left = left;
        this.op = op;
        this.right = right;
    }

    /*
    @Override
    public String toSHErrLocFmt(ScopeContext defContext) {
        String l = left.toSHErrLocFmt(defContext);
        String r = right.toSHErrLocFmt(defContext);
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

     */
//    public String toSHErrLocFmt(String namespace) {
//        String l = left.toSHErrLocFmt(namespace);
//        String r = right.toSHErrLocFmt(namespace);
//        String rnt = "";
//        switch (op) {
//            case JOIN:
//                rnt = "(" + l + " ⊔ " + r + ")";
//                break;
//            case MEET:
//                rnt = "(" + l + " ⊓ " + r + ")";
//                break;
//
//        }
//        return rnt;
//    }

//    public String toSHErrLocFmt(String k, String v) {
//        String l = left.toSHErrLocFmt(k, v);
//        String r = right.toSHErrLocFmt(k, v);
//        String rnt = "";
//        switch (op) {
//            case JOIN:
//                rnt = "(" + l + " ⊔ " + r + ")";
//                break;
//            case MEET:
//                rnt = "(" + l + " ⊓ " + r + ")";
//                break;
//
//        }
//        return rnt;
//    }

//    public String toSherrlocFmtApply(HashSet<String> strSet, int no) {
//        String l = left.toSherrlocFmtApply(strSet, no);
//        String r = right.toSherrlocFmtApply(strSet, no);
//        String rnt = "";
//        switch (op) {
//            case JOIN:
//                rnt = "(" + l + " ⊔ " + r + ")";
//                break;
//            case MEET:
//                rnt = "(" + l + " ⊓ " + r + ")";
//                break;
//
//        }
//        return rnt;
//    }

    public ExpOutcome genConsVisit(VisitEnv env, boolean tail_position) {

        // TODO: deal with dynamic labels
        return null;
    }

//    @Override
//    public void findPrincipal(HashSet<String> principalSet, String getRidOf) {
//        left.findPrincipal(principalSet, getRidOf);
//        right.findPrincipal(principalSet, getRidOf);
//    }

//    @Override
//    public void findPrincipal(HashSet<String> principalSet) {
//        left.findPrincipal(principalSet);
//        right.findPrincipal(principalSet);
//    }

    @Override
    public boolean typeMatch(IfLabel begin_pc) {
        if (!(begin_pc instanceof ComplexIfLabel)) {
            return false;
        }

        ComplexIfLabel cil = (ComplexIfLabel) begin_pc;
        return op == cil.op && left.typeMatch(cil.left) && right.typeMatch(cil.right);
    }

//    public void replace(String k, String v) {
//        left.replace(k, v);
//        right.replace(k, v);
//    }

    @Override
    public boolean typeMatch(Expression expression) {
        return expression instanceof ComplexIfLabel &&
                typeMatch((IfLabel) expression);
    }

    @Override
    public ScopeContext ntcGenCons(NTCEnv env, ScopeContext parent) {
        left.ntcGenCons(env, parent);
        right.ntcGenCons(env, parent);
        return parent;
    }
}

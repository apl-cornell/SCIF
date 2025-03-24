package ast;

import java.util.List;
import typecheck.CodeLocation;
import typecheck.NTCEnv;
import typecheck.ScopeContext;
import typecheck.Utils;
import typecheck.exceptions.SemanticException;

import java.util.ArrayList;

public class FuncLabels extends Node {

    public IfLabel begin_pc, to_pc, gamma_label;
    public IfLabel end_pc;

    public FuncLabels() {
        begin_pc = null;
        to_pc = null;
        gamma_label = null;
        end_pc = null;
    }

    public FuncLabels(IfLabel begin_pc, IfLabel to_pc, IfLabel gamma_label, IfLabel end_pc) {
        this.begin_pc = begin_pc;
        this.to_pc = to_pc;
        this.gamma_label = gamma_label;
        this.end_pc = end_pc;
    }

//    public void findPrincipal(HashSet<String> principalSet) {
//        if (begin_pc != null) {
//            begin_pc.findPrincipal(principalSet);
//        }
//        if (to_pc != null) {
//            to_pc.findPrincipal(principalSet);
//        }
//        if (gamma_label != null) {
//            gamma_label.findPrincipal(principalSet);
//        }
//        if (end_pc != null) {
//            end_pc.findPrincipal(principalSet);
//        }
//    }

    @Override
    public ScopeContext generateConstraints(NTCEnv env, ScopeContext parent) throws SemanticException {
        begin_pc.generateConstraints(env, parent);
        to_pc.generateConstraints(env, parent);
        gamma_label.generateConstraints(env, parent);
        // TODO: set end pc
        if (end_pc != null) end_pc.generateConstraints(env, parent);
        return parent;
    }
//
//    @Override
//    public SolNode solidityCodeGen(CompileEnv code) {
//        assert false;
//    }

    @Override
    public ArrayList<Node> children() {
        ArrayList<Node> rtn = new ArrayList<>();
        rtn.add(begin_pc);
        rtn.add(to_pc);
        rtn.add(gamma_label);
        rtn.add(end_pc);
        return rtn;
    }

    public boolean typeMatch(FuncLabels funcLabels) {
        boolean l1 = begin_pc == null, l2 = to_pc == null, l3 = gamma_label == null, l4 =
                end_pc == null;
        boolean r1 = funcLabels.begin_pc == null, r2 = funcLabels.to_pc == null, r3 =
                funcLabels.gamma_label == null, r4 = end_pc == null;
        return ((l1 && r1) || (!(l1 || r1) && begin_pc.typeMatch(funcLabels.begin_pc)))
                && ((l2 && r2) || (!(l2 || r2) && to_pc.typeMatch(funcLabels.to_pc)))
                && ((l3 && r3) || (!(l3 || r3) && gamma_label.typeMatch(funcLabels.gamma_label)))
                && ((l4 && r4) || (!(l4 || r4) && end_pc.typeMatch(funcLabels.begin_pc)));
    }

    public void setToDefault(boolean isConstructor, List<String> decoratorList, CodeLocation funcLocation) {
        //TODO: set end_pc
        IfLabel thisLbl = new PrimitiveIfLabel(new Name(Utils.LABEL_THIS));
        IfLabel senderLbl = new PrimitiveIfLabel(new Name(Utils.LABEL_SENDER));
        senderLbl.setLoc(funcLocation);
        thisLbl.setLoc(funcLocation);
        if (isConstructor) {
            begin_pc = senderLbl;
            to_pc = gamma_label = end_pc = thisLbl;
            location = funcLocation;
        } else {
            if (gamma_label != null) {
                assert begin_pc != null;
                return;
            } else {
                if (to_pc != null) {
                    assert begin_pc != null;
                    gamma_label = to_pc;
                } else if (begin_pc != null) {
                    gamma_label = to_pc = begin_pc;
                } else {
                    boolean isPublic = false;
                    for (String decorator : decoratorList) {
                        if (decorator.equals(Utils.PUBLIC_DECORATOR)) {
                            isPublic = true;
                            break;
                        }
                    }
                    if (isPublic) {
                        begin_pc = senderLbl;//new PrimitiveIfLabel(new Name(Utils.LABEL_SENDER));
                        to_pc = thisLbl;
                        gamma_label = thisLbl;
                        end_pc = thisLbl;
                        location = funcLocation;
                    } else {
                        begin_pc = to_pc = gamma_label = end_pc = thisLbl;
                        location = funcLocation;
                    }
                }
            }
        }
        // assert !gamma_label.location.fileName.equals("Builtin");
    }
}

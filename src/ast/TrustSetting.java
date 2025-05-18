package ast;

import java.util.Collections;
import java.util.List;
import typecheck.Assumption;
import typecheck.DynamicSystemOption;
import typecheck.InterfaceSym;
import typecheck.NTCEnv;
import typecheck.ScopeContext;
import typecheck.Utils;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import typecheck.VisitEnv;
import typecheck.sherrlocUtils.Relation;

public class TrustSetting extends Node {

    List<TrustConstraint> trust_list;

    DynamicSystemOption dynamicSystemOption;
    HashMap<String, String> labelTable;

    public List<TrustConstraint> trust_list() {
        return Collections.unmodifiableList(trust_list);
    }

    public DynamicSystemOption getDynamicSystemOption() {
        return dynamicSystemOption;
    }

    public Map<String, String> getLabelTable() {
        return Collections.unmodifiableMap(labelTable);
    }

    public TrustSetting() {
        trust_list = new ArrayList<>();
        dynamicSystemOption = null;
        labelTable = new HashMap<>();
    }

    public TrustSetting(List<TrustConstraint> trust_list) {
        this.dynamicSystemOption = null;
        // Utils.resolveDynamicOption(dynamicOption);
        this.trust_list = trust_list;
        this.labelTable = new HashMap<>();
    }

    @Override
    public ScopeContext genTypeConstraints(NTCEnv env, ScopeContext context) {
        for (TrustConstraint trustConstraint : trust_list) {
            trustConstraint.genTypeConstraints(env, context);
        }
        return context;
    }
//
//    @Override
//    public SolNode solidityCodeGen(CompileEnv code) {
//
//    }

    @Override
    public List<Node> children() {
        List<Node> rtn = new ArrayList<>();
        rtn.addAll(trust_list);
        return rtn;
    }

    @Override
    public String toString() {
        return genson.serialize(dynamicSystemOption) + "\n...";
    }

    protected void addBuiltIns() {
        PrimitiveIfLabel labelThis = new PrimitiveIfLabel(new Name(Utils.LABEL_THIS));
        PrimitiveIfLabel labelBot = new PrimitiveIfLabel(new Name(Utils.LABEL_BOTTOM));
        PrimitiveIfLabel labelTop = new PrimitiveIfLabel(new Name(Utils.LABEL_TOP));
        TrustConstraint thisFlowsToBot = new TrustConstraint(labelThis, Relation.LEQ, labelBot);
        TrustConstraint topFlowsToThis = new TrustConstraint(labelTop, Relation.LEQ, labelThis);
//        trust_list.add(thisFlowsToBot);
//        trust_list.add(topFlowsToThis);
    }

    public void genConsVisit(VisitEnv env, boolean tail_position) {

    }

    public void globalInfoVisit(InterfaceSym contractSym) {
        List<Assumption> assumptions = new ArrayList<>();
        for (TrustConstraint constraint : trust_list) {
            assumptions.add(constraint.toAssumption(contractSym));
        }
        contractSym.updateAssumptions(assumptions);
    }
}

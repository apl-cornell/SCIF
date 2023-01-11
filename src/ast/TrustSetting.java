package ast;

import compile.SolCode;
import java.util.List;
import typecheck.DynamicSystemOption;
import typecheck.NTCEnv;
import typecheck.ScopeContext;
import typecheck.Utils;

import java.util.ArrayList;
import java.util.HashMap;
import typecheck.sherrlocUtils.Relation;

public class TrustSetting extends Node {

    List<TrustConstraint> trust_list;

    DynamicSystemOption dynamicSystemOption;
    HashMap<String, String> labelTable;

    public List<TrustConstraint> getTrust_list() {
        //TODO: sus
        return trust_list;
    }

    public DynamicSystemOption getDynamicSystemOption() {
        return dynamicSystemOption;
    }

    public HashMap<String, String> getLabelTable() {
        //TODO: sus
        return labelTable;
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
    public ScopeContext ntcGenCons(NTCEnv env, ScopeContext parent) {
        return null;
    }

    @Override
    public void solidityCodeGen(SolCode code) {

    }

    @Override
    public String toString() {
        return genson.serialize(dynamicSystemOption) + "\n...";
    }

    protected void addBuiltIns() {
        IfLabel labelThis = new PrimitiveIfLabel(new Name(Utils.LABEL_THIS));
        IfLabel labelBot = new PrimitiveIfLabel(new Name(Utils.LABEL_BOTTOM));
        IfLabel labelTop = new PrimitiveIfLabel(new Name(Utils.LABEL_TOP));
        TrustConstraint thisFlowsToBot = new TrustConstraint(labelThis, Relation.LEQ, labelBot);
        TrustConstraint topFlowsToThis = new TrustConstraint(labelTop, Relation.LEQ, labelThis);
        trust_list.add(thisFlowsToBot);
        trust_list.add(topFlowsToThis);
    }
}

package ast;

import compile.SolCode;
import java.util.List;
import typecheck.DynamicSystemOption;
import typecheck.NTCEnv;
import typecheck.ScopeContext;
import typecheck.Utils;

import java.util.ArrayList;
import java.util.HashMap;

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

    public TrustSetting(String dynamicOption, List<TrustConstraint> trust_list,
            HashMap<String, String> labelTable) {
        this.dynamicSystemOption = Utils.resolveDynamicOption(dynamicOption);
        this.trust_list = trust_list;
        this.labelTable = labelTable;
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
}

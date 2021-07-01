package ast;

import typecheck.DynamicSystemOption;
import typecheck.Utils;

import java.util.ArrayList;
import java.util.HashMap;

public class TrustSetting extends Node {
    public ArrayList<TrustConstraint> trust_list;
    public DynamicSystemOption dynamicSystemOption;
    public HashMap<String, String > labelTable;

    public TrustSetting() {
        trust_list = new ArrayList<>();
        dynamicSystemOption = null;
        labelTable = new HashMap<>();
    }

    public TrustSetting(String dynamicOption, ArrayList<TrustConstraint> trust_list, HashMap<String, String> labelTable) {
        this.dynamicSystemOption = Utils.resolveDynamicOption(dynamicOption);
        this.trust_list = trust_list;
        this.labelTable = labelTable;
    }

    @Override
    public String toString() {
        return genson.serialize(dynamicSystemOption) + "\n...";
    }
}

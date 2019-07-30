package utils;

import ast.IfLabel;
import ast.LabeledType;
import ast.Name;

public class TestableVarInfo extends VarInfo {
    public String testedLabel;
    public boolean tested;

    public TestableVarInfo(String varName, TypeInfo type, CodeLocation location, String testedLabel, boolean tested) {
        super(varName, type, location);
        this.testedLabel = testedLabel;
        this.tested = tested;
    }

    public void setTested(String testedLabel) {
        if (testedLabel.equals(Utils.DEAD))
            return;
        tested = true;
        this.testedLabel = testedLabel;
    }

}

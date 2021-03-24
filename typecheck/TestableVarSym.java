package typecheck;

import ast.IfLabel;

public class TestableVarSym extends VarSym {
    public String testedLabel;
    public boolean tested;

    public TestableVarSym(String localName, TypeSym type, IfLabel ifl, CodeLocation location, ScopeContext context, boolean isConst, String testedLabel, boolean tested) {
        super(localName, type, ifl, location, context, isConst);
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

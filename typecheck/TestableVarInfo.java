package typecheck;

public class TestableVarInfo extends VarInfo {
    public String testedLabel;
    public boolean tested;

    public TestableVarInfo(String fullName, String localName, TypeInfo type, CodeLocation location, String testedLabel, boolean tested) {
        super(fullName, localName, type, location);
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

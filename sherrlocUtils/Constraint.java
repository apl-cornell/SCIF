package sherrlocUtils;

import typecheck.CodeLocation;

public class Constraint {
    // names are unique strings
    Inequality inequality;
    Hypothesis hypothesis;
    Position position;
    String contractName;
    String explanation = "";

    /*public Constraint(Inequality inequality, Hypothesis hypothesis, CodeLocation location, String contractName) {
        this.inequality = inequality;
        this.hypothesis = new Hypothesis(hypothesis);
        this.position = location == null ? null : new Position(location);
        this.contractName = contractName;
    }*/

    public Constraint(Inequality inequality, Hypothesis hypothesis, CodeLocation location, String contractName, String explanation) {
        this.inequality = inequality;
        this.hypothesis = new Hypothesis(hypothesis);
        this.position = location == null ? null : new Position(location);
        this.contractName = contractName;
        this.explanation = explanation + "@" + contractName;
    }

    /*public Constraint(Inequality inequality, CodeLocation location, String contractName) {
        this.inequality = inequality;
        this.hypothesis = new Hypothesis();
        this.position = location == null ? null : new Position(location);
        this.contractName = contractName;
    }*/

    public Constraint(Inequality inequality, CodeLocation location, String contractName, String explanation) {
        this.inequality = inequality;
        this.hypothesis = new Hypothesis();
        this.position = location == null ? null : new Position(location);
        this.contractName = contractName;
        this.explanation = explanation + "@" + contractName;
    }

    public Constraint() {
        inequality = null;
        hypothesis = null;
        position = null;
    }

    public String toSherrlocFmt(boolean withPosition) {
        if (inequality == null) {
            return "";
        }
        return inequality.toSherrlocFmt() + " " + hypothesis.toSherrlocFmt()  + ";"
                + (withPosition && position != null ? position.toSherrlocFmt(explanation) : "");
    }
}

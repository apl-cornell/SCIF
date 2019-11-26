package sherrlocUtils;

import typecheck.CodeLocation;

public class Constraint {
    Inequality inequality;
    Hypothesis hypothesis;
    Position position;

    public Constraint(Inequality inequality, Hypothesis hypothesis, Position position) {
        this.inequality = inequality;
        this.hypothesis = new Hypothesis(hypothesis);
        this.position = position;
    }

    public Constraint(Inequality inequality, Hypothesis hypothesis, CodeLocation location) {
        this.inequality = inequality;
        this.hypothesis = new Hypothesis(hypothesis);
        this.position = new Position(location);
    }

    public Constraint(Inequality inequality, CodeLocation location) {
        this.inequality = inequality;
        this.hypothesis = new Hypothesis();
        this.position = new Position(location);
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
                + (withPosition ? position.toSherrlocFmt() : "");
    }
}

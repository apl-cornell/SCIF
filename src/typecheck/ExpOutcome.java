package typecheck;

public class ExpOutcome {
    public String valueLabelName;
    public PathOutcome psi;

    public ExpOutcome(String s, PathOutcome psi) {
        this.valueLabelName = s;
        this.psi = psi;
    }
}

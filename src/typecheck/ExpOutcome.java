package typecheck;

/** The result from information-flow checking of an expression.
 */
public class ExpOutcome {
    public String valueLabelName;
    public PathOutcome psi;

    public ExpOutcome(String s, PathOutcome psi) {
        this.valueLabelName = s;
        this.psi = psi;
    }
}

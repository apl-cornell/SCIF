package typecheck.exceptions;

public class ConstraintCheckFailure extends TypeCheckFailure {
    String log;
    public ConstraintCheckFailure(String log) {
        this.log = log;
    }

    @Override
    public String explanation() {
        return log;
    }
}

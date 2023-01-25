package typecheck.exceptions;

public class NameNotFoundException extends Exception {
    public NameNotFoundException(String name) {
        super(name);
    }

}

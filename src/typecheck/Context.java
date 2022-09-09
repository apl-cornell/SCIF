package typecheck;

public class Context {
    public final String pc;
    public final String lambda;
    // public final String inLockName;

    public Context() {
        pc = null;
        lambda = null;
        // inLockName = null;
    }

    public Context(Context a) {
        this.pc = a.pc;
        this.lambda = a.lambda;
        // this.inLockName = a.inLockName;
    }

    public Context(String outPcName, String lockName) {
        this.pc = outPcName;
        this.lambda = lockName;
        // this.inLockName = inLockName;
    }
}

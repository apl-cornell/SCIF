package compile.ast;

public interface Type {
    public abstract String solCode();

    public default String solCode(boolean isLocal) {
        return solCode();
    }
    public default String solCode(boolean isLocal, boolean isTransient) {
        return isTransient ? solCode(isLocal) + " transient" : solCode(isLocal);
    }
}

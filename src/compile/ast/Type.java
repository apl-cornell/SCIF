package compile.ast;

public interface Type {
    public abstract String solCode();

    public default String solCode(boolean isLocal) {
        return solCode();
    }
}

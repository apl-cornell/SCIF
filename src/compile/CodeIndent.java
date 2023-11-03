package compile;

public class CodeIndent implements CodeToken {
    int indentDelta = 1;
    public CodeIndent() {

    }
    public CodeIndent(int delta) {
        indentDelta = delta;
    }
    public int delta() {
        return indentDelta;
    }
}

package compile;

public class CodeLine implements CodeToken {
    String line;
    public CodeLine(String line) {
        this.line = line;
    }
    public String line() {
        return line;
    }
}

package utils;

public class CodeLocation {
    int lineNo = -1, columnNo = -1;

    public CodeLocation(int lineNo, int columnNo) {
        this.lineNo = lineNo;
        this.columnNo = columnNo;
    }

    public CodeLocation() {
    }

    public boolean valid() {
        return lineNo > 0 && columnNo > 0;
    }

    public String toSherrlocFmt() {
        return lineNo + "," + columnNo + "-" + columnNo;
    }

    @Override
    public String toString() {
        return String.format("L%dC%d", lineNo, columnNo);
    }
}

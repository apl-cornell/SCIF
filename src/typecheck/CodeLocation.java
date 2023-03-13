package typecheck;

public class CodeLocation {
    public int lineNo = 0, columnNo = 0;
    public String fileName = "Builtin";

    public CodeLocation(int lineNo, int columnNo, String fileName) {
        this.lineNo = lineNo;
        this.columnNo = columnNo;
        this.fileName = fileName;
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
        return String.format("L%dC%d", lineNo, columnNo);//, fileName.replace("/", "..."));
    }
}

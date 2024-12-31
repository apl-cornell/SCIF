package typecheck;

import ast.SourceFile;

public class CodeLocation {
    public final int lineNo, columnNo;
    public final String fileName;
    String fileId;

    public CodeLocation(int lineNo, int columnNo, String fileName) {
        this.lineNo = lineNo;
        this.columnNo = columnNo;
        this.fileName = fileName;
        this.fileId = SourceFile.sourceFileNameId(fileName);
    }

    public static CodeLocation builtinCodeLocation() {
        String name ="Builtin";
        return new CodeLocation(0, 0, name);
    }
    public static CodeLocation builtinCodeLocation(int lineNo, int colNo) {
        String name ="Builtin";
        return new CodeLocation(lineNo, colNo, name);
    }

    public boolean valid() {
        return lineNo > 0 && columnNo > 0;
    }

    public String toSherrlocFmt() {
        return lineNo + "," + columnNo + "-" + columnNo;
    }

    @Override
    public String toString() {
        return String.format("L%dC%d", lineNo, columnNo) + fileId;
    }

    public String errString() {
        return fileName + ", line " + lineNo + ", column " + columnNo;
    }
}

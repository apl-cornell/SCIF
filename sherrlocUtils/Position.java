package sherrlocUtils;

import typecheck.CodeLocation;

public class Position {
    int beginLineNo = -1, beginColNo = -1;
    int endLineNo = -1, endColNo = -1;
    String snippet = "";

    public Position(int beginLineNo, int beginColNo) {
        this.beginLineNo = beginLineNo;
        this.beginColNo = beginColNo;
        this.endLineNo = beginLineNo;
        this.endColNo = beginColNo;
    }

    public Position(CodeLocation location) {
        this(location.lineNo, location.columnNo);
    }

    public String toSherrlocFmt() {
        String rtn = "[";
        if (!snippet.equals("")) {
            rtn += snippet + ": ";
        }

        rtn += beginLineNo + "," + beginColNo + "-" + endLineNo + "," + endColNo;

        rtn += "]";

        return rtn;
    }

}

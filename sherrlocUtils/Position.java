package sherrlocUtils;

import typecheck.CodeLocation;

public class Position {
    int beginLineNo = -1, beginColNo = -1;
    int endLineNo = -1, endColNo = -1;
    String snippet = "";
    String fileName;

    public Position(int beginLineNo, int beginColNo, String fileName) {
        this.beginLineNo = beginLineNo;
        this.beginColNo = beginColNo;
        this.endLineNo = beginLineNo;
        this.endColNo = beginColNo;
        this.fileName = fileName;
    }

    public Position(CodeLocation location) {
        this(location.lineNo, location.columnNo, location.fileName);
    }

    public String toSherrlocFmt(String explanation, int weight) {
        String rtn = "[";
        //if (!snippet.equals("")) {
            rtn += "\"" + explanation + "\": ";
        //}

        rtn += beginLineNo + "," + beginColNo + "-" + endLineNo + "," + endColNo;

        rtn += "#" + weight;
        rtn += "]";

        return rtn;
    }

}

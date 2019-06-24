public class IFConstraint {
    public String op;
    public IFLabel left, right;
    public int x, y;
    public IFConstraint(String op, IFLabel left, IFLabel right, int x, int y) {
        this.op = op;
        this.left = left;
        this.right = right;
        this.x = x;
        this.y = y;
    }
    public String toSherrlocFmt() {
        String rnt = "";
        String l = left.toSherrlocFmt();
        String r = right.toSherrlocFmt();
        rnt = l + op + r + "; ";
        if (x >= 0 && y >= 0)
            rnt += "[" + Integer.toString(x) + "," + Integer.toString(y) + "-" + Integer.toString(y) + "]\n";
        else
            rnt += "\n";
        return rnt;
    }
}

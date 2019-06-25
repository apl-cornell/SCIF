
public class VarInfo {
    public String name;
    public IFLabel lbl;
    public int x, y;
    public VarInfo(String name, IFLabel lbl, int x, int y) {
        this.name = name;
        this.lbl = lbl;
        this.x = x;
        this.y = y;
    }
    public String toString() {
        return "VarInfo: {" + name + ", " + lbl.toString() + ", " + Integer.toString(x) + ", " + Integer.toString(y) + "}";
    }
}

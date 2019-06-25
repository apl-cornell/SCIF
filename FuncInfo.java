import java.util.ArrayList;

public class FuncInfo {
    public String name;
    public IFLabel callLbl;
    public ArrayList<VarInfo> prmters;
    public IFLabel rtLbl;
    public int x, y;
    public FuncInfo(String name, IFLabel callLbl, ArrayList<VarInfo> prmters, IFLabel rtLbl, int x, int y) {
        this.name = name;
        this.callLbl = callLbl;
        this.prmters = prmters;
        this.rtLbl = rtLbl;
        this.x = x;
        this.y = y;
    }
    public String toString() {
        return "TODO";
    }
}

import java.util.ArrayList;

public class funcInfo {
    public String name;
    public IFLabel callLbl;
    public ArrayList<varInfo> prmters;
    public IFLabel rtLbl;
    public int x, y;
    public funcInfo(String name, IFLabel callLbl, ArrayList<varInfo> prmters, IFLabel rtLbl, int x, int y) {
        this.name = name;
        this.callLbl = callLbl;
        this.prmters = prmters;
        this.rtLbl = rtLbl;
        this.x = x;
        this.y = y;
    }
}
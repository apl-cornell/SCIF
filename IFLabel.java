

public class IFLabel {
    public String value;
    public IFLabel left = null, right = null;
    public IFLabel(String value) {
        this.value = value;
    }
    public IFLabel(String value, IFLabel left, IFLabel right) {
        this.value = value;
        this.left = left;
        this.right = right;
    }
    public static IFLabel bottom = new IFLabel("_bottom");
}
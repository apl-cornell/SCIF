

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
    public String toSherrlocFmt() {

        if (value.equals("_bottom"))
            return "_";
        else if (value.equals("_top"))
            return "*";
        else if (value.equals("meet") || value.equals("AND"))
            return "(" + left.toSherrlocFmt() + " ⊓ " + right.toSherrlocFmt() + ")";
        else if (value.equals("join") || value.equals("OR"))
            return "(" + left.toSherrlocFmt() + " ⊔ " + right.toSherrlocFmt() + ")";
        else
            return "???";
    }
}

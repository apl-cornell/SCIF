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
}
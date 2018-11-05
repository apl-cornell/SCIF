public class Utils {
    public static IFConstraint genCons(IFLabel left, IFLabel right, int x, int y) {
        return new IFConstraint("<", left, right, x, y);
    }
}

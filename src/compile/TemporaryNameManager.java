package compile;

public class TemporaryNameManager {
    String prefix = "__tmpvar";
    int counter = 0;

    public String newVarName() {
        return prefix + counter++;
    }

}

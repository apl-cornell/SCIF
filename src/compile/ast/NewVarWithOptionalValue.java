package compile.ast;

public class NewVarWithOptionalValue {
    String varName;
    Expression initValue;

    public NewVarWithOptionalValue(String varName) {
        this.varName = varName;
    }

    public NewVarWithOptionalValue(String varName, Expression initValue) {
        this.varName = varName;
        this.initValue = initValue;
    }

    public String toSolCode() {
        return initValue == null ? varName : varName + " = " + initValue.toSolCode();
    }
}

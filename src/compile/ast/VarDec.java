package compile.ast;

import static compile.Utils.addLine;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

public class VarDec implements Statement {
    Type type;
    List<NewVarWithOptionalValue> vars;
    boolean isLocal = true;

    public VarDec(Type type, String name) {
        this.type = type;
        vars = new ArrayList<>();
        vars.add(new NewVarWithOptionalValue(name));
    }
    public VarDec(Type type, String name, Expression initValue) {
        this.type = type;
        vars = new ArrayList<>();
        vars.add(new NewVarWithOptionalValue(name, initValue));
    }

    public VarDec(Type type, List<NewVarWithOptionalValue> vars) {
        this.type = type;
        this.vars = vars;
    }

    public VarDec(boolean isLocal, Type type, String name) {
        this.isLocal = isLocal;
        this.type = type;
        vars = new ArrayList<>();
        vars.add(new NewVarWithOptionalValue(name));
    }
    public VarDec(boolean isLocal, Type type, String name, Expression initValue) {
        this.isLocal = isLocal;
        this.type = type;
        vars = new ArrayList<>();
        vars.add(new NewVarWithOptionalValue(name, initValue));
    }

    @Override
    public List<String> toSolCode(int indentLevel) {
        List<String> result = new ArrayList<>();
        addLine(result, type.solCode(isLocal) + " " + vars.stream().map(var -> var.toSolCode()).collect(
                Collectors.joining(", ")) + ";", indentLevel);
        return result;
    }
}

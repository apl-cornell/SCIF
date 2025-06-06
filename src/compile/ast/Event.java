package compile.ast;

import compile.Utils;

import java.util.ArrayList;
import java.util.List;
import java.util.SimpleTimeZone;
import java.util.stream.Collectors;

public class Event implements SolNode{
    String name;
    List<Argument> args;

    public Event(String name, List<Argument> args) {
        this.name = name;
        this.args = args;
    }

    @Override
    public List<String> toSolCode(int indentLevel) {
        List<String> result = new ArrayList<>();
        Utils.addLine(result, "event " + name + "(" +
                String.join(", ", args.stream().map(
                        arg -> arg.type.solCode(true) + " " + arg.name
                ).collect(Collectors.toList())) + ");", indentLevel);
        return result;
    }

}

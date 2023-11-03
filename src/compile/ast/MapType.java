package compile.ast;

public class MapType implements Type {
    Type from, to;

    public MapType(Type from, Type to) {
        this.from = from;
        this.to = to;
    }

    @Override
    public String solCode() {
        return "mapping(" + from.solCode() + " => " + to.solCode() + ")";
    }
}

package ast;

public class Num<T extends Number> extends Literal {
    T value;
    public Num(T x) {
        value = x;
    }

}

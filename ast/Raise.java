package ast;

public class Raise extends NonFirstLayerStatement {
    Expression exception;
    Expression from;
    public Raise(Expression exception, Expression from) {
        this.exception = exception;
        this.from = from;
    }
    public Raise(Expression exception) {
        this.exception = exception;
    }
    public Raise() {
    }

}

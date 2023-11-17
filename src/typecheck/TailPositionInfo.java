package typecheck;

public class TailPositionInfo {
    Context lastContext;

    public TailPositionInfo() {
    }

    public TailPositionInfo(Context lastContext) {
        this.lastContext = lastContext;
    }

    public boolean tail_position() {
        return lastContext != null;
    }

}

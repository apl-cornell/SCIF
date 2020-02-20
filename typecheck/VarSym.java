package typecheck;

public class VarSym extends Sym{
    public VarInfo varInfo;
    public VarSym(String id, VarInfo varInfo) {
        this.name = id;
        this.varInfo = varInfo;
    }

    public boolean isLValue() {
        return true;
    }
}

package typecheck;

public class FuncSym extends Sym {
    public FuncInfo funcInfo;
    public FuncSym(String id, FuncInfo funcInfo) {
        this.name = id;
        this.funcInfo = funcInfo;
    }
    public boolean isLValue() {
        return false;
    }
}

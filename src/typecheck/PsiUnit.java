package typecheck;

import ast.IfLabel;

public class PsiUnit {
    public Context c;
    public boolean catchable;
    /*public PsiUnit(String pc, String lambda, boolean catchable) {
        this.c = new Context(pc, lambda);
        this.catchable = catchable;
    }*/

    public PsiUnit(String pc, String lambda) {
        this.c = new Context(pc, lambda);
    }

    public PsiUnit(Context endContext) {
        assert endContext != null;
        this.c = endContext;
    }
    public PsiUnit(Context endContext, boolean catchable) {
        assert endContext != null;
        this.c = endContext;
        this.catchable = catchable;
    }

    public Context c() {
        return c;
    }
}

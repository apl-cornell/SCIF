package typecheck;

import ast.IfLabel;

public class PsiUnit {
    public final Context c;
    public boolean catchable;

    public PsiUnit(String pc, String lambda) {
        c = new Context(pc, lambda);
    }

    public PsiUnit(Context endContext) {
        assert endContext != null;
        c = endContext;
    }
    public PsiUnit(Context endContext, boolean catchable) {
        assert endContext != null;
        c = endContext;
        catchable = catchable;
    }

    public Context c() {
        return c;
    }
}

package typecheck;

import jdk.jshell.execution.Util;

import java.nio.file.Path;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

/** The information-flow result from all the possible paths out of evaluation
 * of an expression or statement.
 */
public class PathOutcome {
    public Map<ExceptionTypeSym, PsiUnit> psi;

    public PathOutcome() {
        psi = new HashMap<>();
    }

    public PathOutcome(Map<ExceptionTypeSym, PsiUnit> psi) {
        this.psi = psi;
    }

    public PathOutcome(PsiUnit normalPath) {
        psi = new HashMap<>();
        psi.put(Utils.getNormalPathException(), normalPath);
    }

    public PsiUnit getNormalPath() {
        return psi.get(Utils.getNormalPathException());
    }

    public void set(ExceptionTypeSym key, Context value) {
        set(key, new PsiUnit(value));
    }

    public void set(ExceptionTypeSym key, PsiUnit value) {
        assert value != null;
        psi.put(key, value);
    }
    public void remove(ExceptionTypeSym expSym) {
        assert psi.containsKey(expSym);
        psi.remove(expSym);
    }

    public void setNormalPath(Context endContext) {
        assert endContext != null;
        set(Utils.getNormalPathException(), endContext);
    }

    public void join(PathOutcome other) {
        for (Map.Entry<ExceptionTypeSym, PsiUnit> entry : other.psi.entrySet()) {
            ExceptionTypeSym key = entry.getKey();
            if (psi.containsKey(key) && psi.get(key).c != null) {
                psi.put(key, Utils.joinPsiUnit(psi.get(key), other.psi.get(key)));
            } else {
                psi.put(key, entry.getValue());
            }
        }

    }

    public void joinExe(PathOutcome psi) {
        PsiUnit n = psi.getNormalPath();
        join(psi);
        if (n != null) {
            setNormalPath(n.c);
        }
    }

    public void setReturnPath(Context inContext) {
        assert inContext != null;
        set(Utils.getReturnPathException(), inContext);
    }

    public boolean existsNormalPath() {
        return getNormalPath() != null;
    }

    public void removeNormalPath() {
        remove(Utils.getNormalPathException());
    }
}

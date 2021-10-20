package typecheck;

import ast.Str;
import sherrlocUtils.Constraint;
import sherrlocUtils.Hypothesis;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Set;

public class Context {
    public final String valueLabelName;
    public final String lockName;
    public final String inLockName;

    public Context() {
        valueLabelName = null;
        lockName = null;
        inLockName = null;
    }

    public Context(Context a) {
        this.valueLabelName = a.valueLabelName;
        this.lockName = a.lockName;
        this.inLockName = a.inLockName;
    }

    public Context(String valueLabelName, String lockName, String inLockName) {
        this.valueLabelName = valueLabelName;
        this.lockName = lockName;
        this.inLockName = inLockName;
    }
}

package typecheck;

import ast.Str;
import sherrlocUtils.Constraint;
import sherrlocUtils.Hypothesis;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Set;

public class Context {
    public String valueLabelName;
    public String lockName;

    public Context(String valueLabelName, String lockName) {
        this.valueLabelName = valueLabelName;
        this.lockName = lockName;
    }
}

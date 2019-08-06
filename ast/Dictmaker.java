package ast;

import sherrlocUtils.Constraint;
import sherrlocUtils.Inequality;
import typecheck.*;

import java.util.ArrayList;
import java.util.HashMap;

public class Dictmaker extends Expression {
    ArrayList<Expression> keys, values;
    public Dictmaker(ArrayList<Expression> keys, ArrayList<Expression> values) {
        this.keys = keys;
        this.values = values;
    }
    public Dictmaker() {
        this.keys = new ArrayList<>();
        this.values = new ArrayList<>();
    }
    public void addPair(Expression key, Expression value) {
        this.keys.add(key);
        this.values.add(value);
    }
    @Override
    public String genConsVisit(VisitEnv env) {
        String ifNameRtn = env.ctxt + "." + "dictmaker" + location.toString();
        for (Expression value: values) {
            String ifNameValue = value.genConsVisit(env);
            env.cons.add(new Constraint(new Inequality(ifNameValue, ifNameRtn), env.hypothesis, location));

        }
        return ifNameRtn;
    }
}

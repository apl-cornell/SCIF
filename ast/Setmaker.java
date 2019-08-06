package ast;

import sherrlocUtils.Constraint;
import sherrlocUtils.Inequality;
import typecheck.*;

import java.util.ArrayList;
import java.util.HashMap;

public class Setmaker extends Expression {
    ArrayList<Expression> elements;
    public Setmaker(ArrayList<Expression> elements) {
        this.elements = elements;
    }
    public Setmaker() {
        this.elements = new ArrayList<>();
    }
    public void addElement(Expression element) {
        this.elements.add(element);
    }
    @Override
    public String genConsVisit(VisitEnv env) {
        String ifNameRtn = env.ctxt + "." + "setmaker" + location.toString();
        for (Expression value: elements) {
            String ifNameValue = value.genConsVisit(env);
            env.cons.add(new Constraint(new Inequality(ifNameValue, ifNameRtn), env.hypothesis, location));

        }
        return ifNameRtn;
    }
}

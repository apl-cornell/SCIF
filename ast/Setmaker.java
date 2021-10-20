package ast;

import sherrlocUtils.Constraint;
import sherrlocUtils.Inequality;
import sherrlocUtils.Relation;
import typecheck.*;

import java.util.ArrayList;

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
    public Context genConsVisit(VisitEnv env, boolean tail_position) {
        Context context = env.context;
        Context curContext = new Context(context.valueLabelName, Utils.getLabelNameLock(location), context.inLockName);

        String ifNameRtn = scopeContext.getSHErrLocName() + "." + "setmaker" + location.toString();
        // String prevLock = env.prevContext.lockName;
        // Context lasttmp = null;
        int index = 0;
        for (Expression value: elements) {
            ++index;
            /*if (lasttmp != null) {
                env.cons.add(new Constraint(new Inequality(prevLock, Relation.EQ, lasttmp.lockName), env.hypothesis, location, env.curContractSym.name,
                        Utils.ERROR_MESSAGE_LOCK_IN_NONLAST_OPERATION));
                env.prevContext.lockName = prevLock;
            }*/
            env.context = curContext;
            Context tmp = value.genConsVisit(env, tail_position && index == elements.size());
            String ifNameValue = tmp.valueLabelName;
            env.cons.add(new Constraint(new Inequality(ifNameValue, ifNameRtn), env.hypothesis, location, env.curContractSym.name,
                    "Value must be trusted to make a set"));
            // lasttmp = tmp;
        }
        return curContext;
    }

    @Override
    public boolean typeMatch(Expression expression) {
        return expression instanceof Setmaker &&
                Utils.arrayExpressionTypeMatch(elements, ((Setmaker) expression).elements);
    }
}

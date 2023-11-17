package typecheck.sherrlocUtils;

import java.util.ArrayList;
import java.util.List;
import java.util.Stack;

public class Hypothesis {
    Stack<Inequality> inequalities;
    Stack<Integer> scopePos;
    public Hypothesis() {
        this.inequalities = new Stack<>();
        this.scopePos = new Stack<>();
    }
//    public Hypothesis(Inequality inequality) {
//        this.inequalities = new ArrayList<>();
//        this.inequalities.add(inequality);
//    }
//    public Hypothesis(ArrayList<Inequality> inequalities) {
//        this.inequalities = inequalities;
//    }

    public Hypothesis(Hypothesis hypothesis) {
        inequalities = new Stack<>();
        inequalities.addAll(hypothesis.inequalities);
    }

    public String toSherrlocFmt() {
        if (inequalities.isEmpty())
            return "";

        String rtn = "{";
        for (Inequality inequality : inequalities) {
            rtn += inequality.toSherrlocFmt() + "; ";
        }
        rtn += "}";

        return rtn;
    }

    public void add(Inequality inequality) {
        inequalities.add(inequality);
    }

    public void pop() {
        inequalities.pop();
    }

    public void enterScope() {
        scopePos.push(inequalities.size());
    }

    public void exitScope() {
        while (inequalities.size() > scopePos.peek()) {
            inequalities.pop();
        }
        scopePos.pop();
    }

    public boolean empty() {
        return inequalities.isEmpty();
    }
}

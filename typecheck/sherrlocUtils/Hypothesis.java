package sherrlocUtils;

import java.util.ArrayList;

public class Hypothesis {
    ArrayList<Inequality> inequalities;
    public Hypothesis() {
        this.inequalities = new ArrayList<>();
    }
    public Hypothesis(Inequality inequality) {
        this.inequalities = new ArrayList<>();
        this.inequalities.add(inequality);
    }
    public Hypothesis(ArrayList<Inequality> inequalities) {
        this.inequalities = inequalities;
    }

    public Hypothesis(Hypothesis hypothesis) {
        inequalities = new ArrayList<>();
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

    public void remove() {
        inequalities.remove(inequalities.size() - 1);
    }
}

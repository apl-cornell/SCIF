package ast;

import java.util.ArrayList;

public class Delete extends NonFirstLayerStatement {
    ArrayList<Expression> targets;
    public Delete(ArrayList<Expression> targets) {
        this.targets = targets;
    }
}

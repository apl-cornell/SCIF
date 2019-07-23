package ast;

import utils.CodeLocation;

import java.util.ArrayList;

public class Delete extends Statement {
    ArrayList<Expression> targets;
    public Delete(ArrayList<Expression> targets) {
        this.targets = targets;
    }
}

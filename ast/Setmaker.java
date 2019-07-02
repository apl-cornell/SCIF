package ast;

import utils.FuncInfo;
import utils.IfConstraint;
import utils.LookupMaps;
import utils.Utils;

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
    public String genConsVisit(String ctxt, HashMap<String, FuncInfo> funcMap, ArrayList<IfConstraint> cons, LookupMaps varNameMap) {
        String ifNameRnt = ctxt + "." + "setmaker" + location.toString();
        for (Expression value: elements) {
            String ifNameValue = value.genConsVisit(ctxt, funcMap, cons, varNameMap);
            cons.add(Utils.genCons(ifNameRnt, ifNameValue, location));
        }
        return ifNameRnt;
    }
}

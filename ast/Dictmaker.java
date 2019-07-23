package ast;

import utils.*;

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
    public String genConsVisit(String ctxt, HashMap<String, FuncInfo> funcMap, ArrayList<IfConstraint> cons, LookupMaps varNameMap) {
        String ifNameRnt = ctxt + "." + "dictmaker" + location.toString();
        for (Expression value: values) {
            String ifNameValue = value.genConsVisit(ctxt, funcMap, cons, varNameMap);
            cons.add(Utils.genCons(ifNameValue, ifNameRnt, location));
        }
        return ifNameRnt;
    }
}

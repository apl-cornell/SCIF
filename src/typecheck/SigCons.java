package typecheck;

import typecheck.sherrlocUtils.Constraint;

import java.util.ArrayList;

public class SigCons {
    public String name;
    public ArrayList<Constraint> trustcons, cons;

    public SigCons(String name, ArrayList<Constraint> trustCons, ArrayList<Constraint> cons) {
        this.name = name;
        this.trustcons = trustCons;
        this.cons = cons;
    }
}

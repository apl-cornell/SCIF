package ast;

import java.util.HashSet;

public class Type extends Expression {
    public String x;
    public Type(String x) {
        this.x = x;
    }

    public String toSherrloc(String k, String v) {
        return "";
    }

    @Override
    public void findPrincipal(HashSet<String> principalSet) {
        return;
    }


    public void findPrincipal(HashSet<String> principalSet, String getRidOf) {
        return;
    }
}

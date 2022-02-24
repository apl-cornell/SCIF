package ast;

import java.util.HashSet;

public abstract class Statement extends Node {

    public abstract void findPrincipal(HashSet<String> principalSet);

    public String toString() {
        return "";
    }
}

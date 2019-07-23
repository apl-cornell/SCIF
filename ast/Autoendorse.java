package ast;

import utils.CodeLocation;

public class Autoendorse extends IfLabel {
    public IfLabel from, to;
    public Autoendorse(IfLabel from, IfLabel to) {
        this.from = from;
        this.to = to;
    }


}

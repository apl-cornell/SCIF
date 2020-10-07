package ast;

import compile.SolCode;

public class Break extends NonFirstLayerStatement {
    @Override
    public void SolCodeGen(SolCode code) {
        code.addBreak();
    }
}

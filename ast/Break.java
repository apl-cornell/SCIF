package ast;

import compile.SolCode;

public class Break extends Statement {
    @Override
    public void SolCodeGen(SolCode code) {
        code.addBreak();
    }
}

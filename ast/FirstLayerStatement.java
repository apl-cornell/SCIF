package ast;

import typecheck.ContractSym;

import java.util.HashSet;

public abstract class FirstLayerStatement extends Statement {

    public abstract void globalInfoVisit(ContractSym contractSym);
    /*@Override
    public void findPrincipal(HashSet<String> principalSet) {
        // should never be call
        logger.debug("be called unexpectedly");
    }*/
}

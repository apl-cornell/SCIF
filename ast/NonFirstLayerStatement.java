package ast;

import java.util.HashSet;

public abstract class NonFirstLayerStatement extends Statement {
    @Override
    public void findPrincipal(HashSet<String> principalSet) {
        // should never be call
        logger.debug("be called unexpectedly");
    }
}

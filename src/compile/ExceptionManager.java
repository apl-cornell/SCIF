package compile;

import java.util.HashMap;
import java.util.Map;
import typecheck.ExceptionTypeSym;

public class ExceptionManager {
    int counter;
    boolean exceptionFree;
    Map<ExceptionTypeSym, Integer> exceptionIDs;

    public ExceptionManager() {
        counter = 0;
        exceptionFree = true;
        exceptionIDs = new HashMap<>();
    }

    public int id(ExceptionTypeSym exceptionTypeSym) {
        if (!exceptionIDs.containsKey(exceptionTypeSym)) {
            // assigning new id to a new exception type, starting from 1
            exceptionIDs.put(exceptionTypeSym, ++counter);
        }
        return exceptionIDs.get(exceptionTypeSym);
    }

    public boolean exceptionFree() {
        return exceptionFree;
    }

    public void removeExceptionFree() {
        exceptionFree = false;
    }

    public void setExceptionFree() {
        exceptionFree = true;
    }
}

import ast.Node;
import compile.SolCode;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.util.ArrayList;

public class SolCompiler {
    public static void compile(ArrayList<Node> roots, File outputFile) {
        // assuming the code typechecks, might need to deal with namespace when multi-contract
        //TODO: assuming only one contract for now
        logger.trace("compile starts");

        System.out.println("\nCompiled Solidity code:");
        for (Node root : roots) {
            SolCode code = new SolCode();
            root.SolCodeGen(code);

            code.output(outputFile);
            code.display();
        }

        logger.trace("compile finished");
    }

    protected static final Logger logger = LogManager.getLogger();
}
 
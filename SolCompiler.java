import ast.SourceFile;
import compile.SolCode;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.File;
import java.util.List;

public class SolCompiler {

    public static void compile(List<SourceFile> roots, File outputFile) {
        // assuming the code typechecks, might need to deal with namespace when multi-contract
        //TODO: assuming only one contract for now
        logger.trace("compile starts");

        System.out.println("\nCompiled Solidity code:");
        for (SourceFile root : roots) {
            SolCode code = new SolCode();
            root.solidityCodeGen(code);

            code.output(outputFile);
            code.display();
        }

        logger.trace("compile finished");
    }

    protected static final Logger logger = LogManager.getLogger();
}
 
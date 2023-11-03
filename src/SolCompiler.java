import ast.SourceFile;
import compile.CompileEnv;
import compile.Utils;
import compile.ast.SolNode;
import java.util.Map;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.File;
import java.util.List;
import typecheck.ExceptionTypeSym;

public class SolCompiler {

    public static void compile(SourceFile root, File outputFile) {
        // assuming the code typechecks, might need to deal with namespace when multi-contract
        logger.trace("compiling starts");

        System.out.println("\nCompiled Solidity code:");

            CompileEnv env = new CompileEnv();
            // System.err.println("Compiling " + root.getContractName() + ":");
            SolNode node = root.solidityCodeGen(env);

            Utils.writeToFile(node, outputFile);
            Utils.printCode(node);

        logger.trace("compiling finished");
    }

    protected static final Logger logger = LogManager.getLogger();
}
 
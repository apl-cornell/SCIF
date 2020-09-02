// the main class of STC

import ast.Node;
import ast.Str;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import picocli.CommandLine;
import picocli.CommandLine.*;
import typecheck.Utils;

import java.io.File;
import java.util.ArrayList;
import java.util.concurrent.Callable;

@Command(name = "STC", version = "STC 0.1.0", mixinStandardHelpOptions = true,
        description = "A set of tools for a new smart contract language with information flow control, SCIF-core.")
public class STC implements Callable<Integer> {
    @Option(names = "-i", arity = "1..*", required = true, description = "The source code file(s).")
    File[] inputFiles;

    @Option(names = "-lg", arity = "1..*", description = "The log file.")
    String[] outputFileNames;

    @ArgGroup(exclusive = true, multiplicity = "1")
    FuncRequest funcRequest;

    static class FuncRequest {
        @Option(names = {"-t", "--typechecker"}, required = true, description = "Information flow typecheck: constraints as log")
        boolean typecheck;
        @Option(names = {"-p", "--parser"}, required = true, description = "Parse: ast json as log")
        boolean parse;
        @Option(names = {"-l", "--lexer"}, required = true, description = "Tokenize")
        boolean tokenize;
    }

    public static void main(String[] args) {
        int exitCode = new CommandLine(new STC()).execute(args);
        System.exit(exitCode);
    }

    ArrayList<Node> typecheck(String[] outputFileNames) throws Exception {
        String outputFileName;
        File outputFile;
        if (outputFileNames == null) {
            outputFile = File.createTempFile("cons", "tmp");
            outputFileName = outputFile.getAbsolutePath();
            // outputFile.deleteOnExit();
        } else {
            outputFileName = outputFileNames[0];
            outputFile = new File(outputFileName);
        }
        ArrayList<File> files = new ArrayList<>();
        for (File file : inputFiles) {
            files.add(file);
        }
        ArrayList<Node> roots = TypeChecker.regularTypecheck(files, outputFile);
        System.out.println("Regular Typechecking:");
        boolean passNTC = true;
        if (!Utils.emptyFile(outputFileName))
            passNTC = runSLC(outputFileName);
        System.out.println("["+ outputFileName + "]" + "Information Flow Typechecking:");
        TypeChecker.ifcTypecheck(roots, outputFile);
        System.out.println("["+ outputFileName + "]" + "Information Flow Typechecking finished");
        logger.debug("running SHErrLoc...");
        boolean passIFC = runSLC(outputFileName);

        return (passNTC && passIFC) ? roots : null;
    }

    @Override
    public Integer call() throws Exception {
        logger.trace("STC starts");
        if (funcRequest.typecheck) {
            typecheck(outputFileNames);
        } else if (funcRequest.parse) {
            File astOutputFile = outputFileNames == null ? null : new File(outputFileNames[0]);
            Parser.parse(inputFiles[0], astOutputFile);
        } else if (funcRequest.tokenize) {
            LexerTest.tokenize(inputFiles[0]);
        } else {
                logger.error("No funcRequest specified, this should never happen!");
                //System.out.println("No funcRequest specified, this should never happen!");
        }

        logger.trace("STC finishes");
        return 0;
    }

    boolean runSLC(String outputFileName) throws Exception {

        String classDirectoryPath = new File(STC.class.getProtectionDomain().getCodeSource().getLocation().toURI()).getPath();
        String[] sherrlocResult = Utils.runSherrloc(classDirectoryPath, outputFileName);
        logger.debug(sherrlocResult);
        if (sherrlocResult.length < 1) {
            System.out.println(Utils.TYPECHECK_NORESULT_MSG);
            return false;
        } else if (sherrlocResult[sherrlocResult.length - 1].contains(Utils.SHERRLOC_PASS_INDICATOR)) {
            System.out.println(Utils.TYPECHECK_PASS_MSG);
        } else {
            System.out.println(Utils.TYPECHECK_ERROR_MSG);
            for (int i = 0; i < sherrlocResult.length; ++i)
                if (sherrlocResult[i].contains(Utils.SHERRLOC_ERROR_INDICATOR)) {
                    for (int j = i; j < sherrlocResult.length; ++j) {
                        System.out.println(sherrlocResult[j]);
                    }
                    break;
                }
            return false;
        }
        return true;
    }

    protected static final Logger logger = LogManager.getLogger();
}

// the main class of STC

import ast.Node;
import ast.Program;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import picocli.CommandLine;
import picocli.CommandLine.*;
import typecheck.Utils;

import java.io.File;
import java.util.ArrayList;
import java.util.concurrent.Callable;

@Command(name = "SCIF", version = "SCIF 0.1.0", mixinStandardHelpOptions = true,
        description = "A set of tools for a new smart contract language with information flow control, SCIF.")
public class SCIF implements Callable<Integer> {
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
        @Option(names = {"-c", "--compiler"}, required = true, description = "Compile to Solidity")
        boolean compile;
    }

    public static void main(String[] args) {
        int exitCode = new CommandLine(new SCIF()).execute(args);
        System.exit(exitCode);
    }

    ArrayList<Program> typecheck(String[] outputFileNames) throws Exception {
        File NTCConsFile;
        if (outputFileNames.length <= 0) {
            NTCConsFile = File.createTempFile("cons", "tmp");
            // outputFile.deleteOnExit();
        } else {
            NTCConsFile = new File(outputFileNames[0]);
        }
        ArrayList<File> files = new ArrayList<>();
        for (File file : inputFiles) {
            files.add(file);
        }
        System.out.println("Regular Typechecking:");
        ArrayList<Program> roots = TypeChecker.regularTypecheck(files, NTCConsFile);
        boolean passNTC = true;
        //if (!Utils.emptyFile(outputFileName))
        //    passNTC = runSLC(outputFileName);
        if (roots == null)
            passNTC = false;

        if (!passNTC) return null;
        // System.out.println("["+ outputFileName + "]");
        ArrayList<File> IFCConsFiles = new ArrayList<>();
        for (int i = 0; i < roots.size(); ++i) {
            File IFCConsFile;
            if (outputFileNames.length <= i + 1) {
                IFCConsFile = File.createTempFile("cons", "tmp");
            } else {
                IFCConsFile = new File(outputFileNames[i + 1]);
            }
            IFCConsFiles.add(IFCConsFile);
        }

        System.out.println("\nInformation Flow Typechecking:");
        boolean passIFC = TypeChecker.ifcTypecheck(roots, IFCConsFiles);
        // System.out.println("["+ outputFileName + "]" + "Information Flow Typechecking finished");
        // logger.debug("running SHErrLoc...");
        // boolean passIFC = runSLC(outputFileName);

        return (passNTC && passIFC) ? roots : null;
    }

    @Override
    public Integer call() throws Exception {

        logger.trace("SCIF starts");
        if (funcRequest.typecheck) {
            typecheck(outputFileNames);
        } else if (funcRequest.parse) {
            File astOutputFile = outputFileNames == null ? null : new File(outputFileNames[0]);
            Parser.parse(inputFiles[0], astOutputFile);
        } else if (funcRequest.tokenize) {
            LexerTest.tokenize(inputFiles[0]);
        } else if (funcRequest.compile) {
            ArrayList<Program> roots = typecheck(new String[0]);
            logger.debug("finished typecheck, compiling...");
            if (roots == null) {
                return 1;
            }
            String outputFileName;
            File outputFile;
            if (outputFileNames == null) {
                outputFile = File.createTempFile("tmp", "sol");
                outputFileName = outputFile.getAbsolutePath();
                outputFile.deleteOnExit();
            } else {
                outputFileName = outputFileNames[0];
                outputFile = new File(outputFileName);
            }
            SolCompiler.compile(roots, outputFile);
        } else {
                logger.error("No funcRequest specified, this should never happen!");
                //System.out.println("No funcRequest specified, this should never happen!");
        }

        logger.trace("SCIF finishes");
        return 0;
    }

    protected static final Logger logger = LogManager.getLogger();
}

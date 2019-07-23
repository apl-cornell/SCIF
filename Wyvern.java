// the main class of Wyvern

import picocli.CommandLine;
import picocli.CommandLine.*;
import utils.Utils;

import java.io.File;
import java.io.UTFDataFormatException;
import java.util.concurrent.Callable;

@Command(name = "wyvern", version = "wyvern 0.1.0", mixinStandardHelpOptions = true,
        description = "A set of tools for a new smart contract language with information flow control, Vyperflow.")
public class Wyvern implements Callable<Integer> {
    @Parameters(index = "0", description = "The source code file.")
    File inputFile;

    @Parameters(index = "1..*", description = "The log file.")
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
        int exitCode = new CommandLine(new Wyvern()).execute(args);
        System.exit(exitCode);
    }

    @Override
    public Integer call() throws Exception {
        if (funcRequest.typecheck) {
            String outputFileName;
            File outputFile;
            if (outputFileNames == null) {
                outputFile = File.createTempFile("cons", "tmp");
                outputFileName = outputFile.getAbsolutePath();
                outputFile.deleteOnExit();
            } else {
                outputFileName = outputFileNames[0];
                outputFile = new File(outputFileName);
            }
            TypeChecker.typecheck(inputFile, outputFile);
            String classDirectoryPath = new File(Wyvern.class.getProtectionDomain().getCodeSource().getLocation().toURI()).getPath();
            String[] sherrlocResult = Utils.runSherrloc(classDirectoryPath, outputFileName);
            if (sherrlocResult[sherrlocResult.length - 1].contains(Utils.SHERRLOC_PASS_INDICATOR)) {
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
            }
        } else if (funcRequest.parse) {
            File astOutputFile = outputFileNames == null ? null : new File(outputFileNames[0]);
            Parser.parse(inputFile, astOutputFile);
        } else if (funcRequest.tokenize) {
            LexerTest.tokenize(inputFile);
        } else {
                System.out.println("No funcRequest specified, this should never happen!");
        }
        return 0;
    }
}

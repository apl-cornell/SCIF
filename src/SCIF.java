// the main class of STC

import ast.SourceFile;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import picocli.CommandLine;
import picocli.CommandLine.*;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Callable;


@Command(name = "SCIF", version = "SCIF 0.1.0", mixinStandardHelpOptions = true,
        description = "A set of tools for a new smart contract language with information flow control, SCIF.")
public class SCIF implements Callable<Integer> {

    @Parameters(arity = "1..*", description = "The source code file(s).")
    private File[] m_inputFiles;

    @Option(names = "-debug")
    private boolean m_debug;

    @Option(names = "-lg", arity = "1..*", description = "The log file.")
    private String[] m_outputFileNames;

    @ArgGroup(exclusive = true)
    private FuncRequest m_funcRequest;

    private static class FuncRequest {

        @Option(names = {"-t",
                "--typechecker"}, required = true, description = "Information flow typecheck: constraints as log")
        boolean typecheck;
        @Option(names = {"-p", "--parser"}, required = true, description = "Parse: ast json as log")
        boolean parse;
        @Option(names = {"-l", "--lexer"}, required = true, description = "Tokenize")
        boolean tokenize;
        @Option(names = {"-c",
                "--compiler"}, required = true, description = "Compile to Solidity (default)")
        boolean compile;
    }

    public static void main(String[] args) {
        int exitCode = new CommandLine(new SCIF()).execute(args);
        System.exit(exitCode);
    }

    private List<SourceFile> _typecheck(String[] outputFileNames) throws Exception {
        File logDir = new File("./.scif");
        logDir.mkdirs();

        File NTCConsFile;
        if (outputFileNames == null || outputFileNames.length <= 0) {
            NTCConsFile = new File(logDir, "ntc.cons");
            // outputFile.deleteOnExit();
        } else {
            NTCConsFile = new File(outputFileNames[0]);
        }

        //List<File> files = ImmutableList.copyOf(m_inputFiles);
        ArrayList<File> files = new ArrayList<>();
        for (File file : m_inputFiles) {
            files.add(file);
        }
        System.out.println("Regular Typechecking:");
        List<SourceFile> roots = TypeChecker.regularTypecheck(files, NTCConsFile, m_debug);
        boolean passNTC = true;
        //if (!Utils.emptyFile(outputFileName))
        //    passNTC = runSLC(outputFileName);
        if (roots == null) {
            passNTC = false;
        }

        if (!passNTC) {
            return null;
        }
        if (3 == 3) {
            return null;
        }
        // System.out.println("["+ outputFileName + "]");
        ArrayList<File> IFCConsFiles = new ArrayList<>();
        for (int i = 0; i < roots.size(); ++i) {
            File IFCConsFile;
            if (outputFileNames == null || outputFileNames.length <= i + 1) {
                IFCConsFile = new File(logDir, "ifc" + i + ".cons");
            } else {
                IFCConsFile = new File(outputFileNames[i + 1]);
            }
            IFCConsFiles.add(IFCConsFile);
        }

        System.out.println("\nInformation Flow Typechecking:");
        boolean passIFC = TypeChecker.ifcTypecheck(roots, IFCConsFiles, m_debug);
        // System.out.println("["+ outputFileName + "]" + "Information Flow Typechecking finished");
        // logger.debug("running SHErrLoc...");
        // boolean passIFC = runSLC(outputFileName);

        return (passNTC && passIFC) ? roots : null;
    }

    @Override
    public Integer call() throws Exception {

        logger.trace("SCIF starts");
        if (m_funcRequest == null || m_funcRequest.compile) {
            List<SourceFile> roots = _typecheck(new String[0]);
            logger.debug("finished typecheck, compiling...");
            if (roots == null) {
                return 1;
            }
            String outputFileName;
            File outputFile;
            if (m_outputFileNames == null) {
                outputFile = File.createTempFile("tmp", "sol");
                outputFileName = outputFile.getAbsolutePath();
                outputFile.deleteOnExit();
            } else {
                outputFileName = m_outputFileNames[0];
                outputFile = new File(outputFileName);
            }
            SolCompiler.compile(roots, outputFile);
        } else if (m_funcRequest.typecheck) {
            _typecheck(m_outputFileNames);
        } else if (m_funcRequest.parse) {
            File astOutputFile = m_outputFileNames == null ? null : new File(m_outputFileNames[0]);
            Parser.parse(m_inputFiles[0], astOutputFile);
        } else if (m_funcRequest.tokenize) {
            LexerTest.tokenize(m_inputFiles[0]);
        } else {
            logger.error("No funcRequest specified, this should never happen!");
            //System.out.println("No funcRequest specified, this should never happen!");
        }

        logger.trace("SCIF finishes");
        return 0;
    }

    protected static final Logger logger = LogManager.getLogger(SCIF.class);
}

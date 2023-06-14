import static org.junit.jupiter.api.Assertions.assertNotNull;

import ast.SourceFile;
import java.io.File;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

public class TestCompilation {
    boolean m_debug = true;

    @ParameterizedTest
    @ValueSource(strings = {
            "basic/EmptyContract",
    })
    void testPositive(String contractName) {
        File logDir = new File("./.scif");
        logDir.mkdirs();
        String inputFilePath = contractName + ".scif";
        URL input = ClassLoader.getSystemResource(inputFilePath);
        System.out.println(inputFilePath + ": " + input);
        File ntcConsFile = new File(logDir, "ntc.cons");
        List<File> files = new ArrayList<>();
        files.add(new File(input.getFile()));
        List<SourceFile> roots = null;
        try {
            roots = TypeChecker.regularTypecheck(files, ntcConsFile, m_debug);
        } catch (Exception e) {
            e.printStackTrace();
            assert false;
        }
        assertNotNull(roots);
        // System.out.println("["+ outputFileName + "]");
        List<File> ifcConsFiles = new ArrayList<>();
        for (int i = 0; i < roots.size(); ++i) {
            File IFCConsFile;
            IFCConsFile = new File(logDir, "ifc" + i + ".cons");
            ifcConsFiles.add(IFCConsFile);
        }

        System.out.println("\nInformation Flow Typechecking:");

        boolean passIFC = false;
        try {
            passIFC = TypeChecker.ifcTypecheck(roots, ifcConsFiles, m_debug);
        } catch (Exception exp) {
            exp.printStackTrace();
        }
        // System.out.println("["+ outputFileName + "]" + "Information Flow Typechecking finished");
        // logger.debug("running SHErrLoc...");
        // boolean passIFC = runSLC(outputFileName);

        assert passIFC;


        try {
            File outputFile = File.createTempFile("tmp", "sol");
            outputFile.deleteOnExit();
            SolCompiler.compile(roots, outputFile);
        } catch (Exception exp) {
            exp.printStackTrace();
            assert false;
        }
    }
}

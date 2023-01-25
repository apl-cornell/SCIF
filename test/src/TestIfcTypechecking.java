import static org.junit.jupiter.api.Assertions.assertNotNull;

import ast.SourceFile;
import java.io.File;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import java_cup.runtime.Symbol;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

public class TestIfcTypechecking {
    boolean m_debug = true;

    @ParameterizedTest
    @ValueSource(strings = {
            "basic/EmptyContract",
            "basic/ExceptionThrowAndCatch",
            "ifcTypechecking/LocalTrust",
            "ifcTypechecking/Wallet_lock_exception",
            "basic/Seq",
            // "examples/ERC20"
    })
    void testPositiveIfcTypechecking(String contractName) {
        File logDir = new File("./.scif");
        logDir.mkdirs();
        String inputFilePath = contractName + ".scif";
        URL input = ClassLoader.getSystemResource(inputFilePath);
        System.out.println(inputFilePath + ": " + input);
        File NTCConsFile = new File(logDir, "ntc.cons");
        ArrayList<File> files = new ArrayList<>();
        files.add(new File(input.getFile()));
        List<SourceFile> roots = TypeChecker.regularTypecheck(files, NTCConsFile, m_debug);
        assertNotNull(roots);
        // System.out.println("["+ outputFileName + "]");
        ArrayList<File> IFCConsFiles = new ArrayList<>();
        for (int i = 0; i < roots.size(); ++i) {
            File IFCConsFile;
            IFCConsFile = new File(logDir, "ifc" + i + ".cons");
            IFCConsFiles.add(IFCConsFile);
        }

        System.out.println("\nInformation Flow Typechecking:");

        boolean passIFC = false;
        try {
            passIFC = TypeChecker.ifcTypecheck(roots, IFCConsFiles, m_debug);
        } catch (Exception exp) {
            exp.printStackTrace();
        }
        // System.out.println("["+ outputFileName + "]" + "Information Flow Typechecking finished");
        // logger.debug("running SHErrLoc...");
        // boolean passIFC = runSLC(outputFileName);

        assert passIFC;
    }
    @ParameterizedTest
    @ValueSource(strings = {
            "ifcTypechecking/LocalTrust_W01",
            "ifcTypechecking/Wallet_lock_exception_W01",
            "ifcTypechecking/Wallet_lock_exception_W02",
            // "examples/ERC20"
    })
    void testNegtiveIfcTypechecking(String contractName) {
        File logDir = new File("./.scif");
        logDir.mkdirs();
        String inputFilePath = contractName + ".scif";
        URL input = ClassLoader.getSystemResource(inputFilePath);
        System.out.println(inputFilePath + ": " + input);
        File NTCConsFile = new File(logDir, "ntc.cons");
        ArrayList<File> files = new ArrayList<>();
        files.add(new File(input.getFile()));
        List<SourceFile> roots = TypeChecker.regularTypecheck(files, NTCConsFile, m_debug);
        assertNotNull(roots);
        // System.out.println("["+ outputFileName + "]");
        ArrayList<File> IFCConsFiles = new ArrayList<>();
        for (int i = 0; i < roots.size(); ++i) {
            File IFCConsFile;
            IFCConsFile = new File(logDir, "ifc" + i + ".cons");
            IFCConsFiles.add(IFCConsFile);
        }

        System.out.println("\nInformation Flow Typechecking:");

        boolean passIFC = false;
        try {
            passIFC = TypeChecker.ifcTypecheck(roots, IFCConsFiles, m_debug);
        } catch (Exception exp) {
            exp.printStackTrace();
        }
        // System.out.println("["+ outputFileName + "]" + "Information Flow Typechecking finished");
        // logger.debug("running SHErrLoc...");
        // boolean passIFC = runSLC(outputFileName);

        assert !passIFC;
    }
}

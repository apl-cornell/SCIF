import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;

import ast.SourceFile;
import java.io.File;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import java_cup.runtime.Symbol;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

public class TestRegularTypechecking {
    private boolean m_debug = true;

    @ParameterizedTest
    @ValueSource(strings = {
            "basic/DependentMap",
            "basic/EmptyContract",
            "basic/ExceptionThrowAndCatch",
            "basic/FinalVar",
            "basic/EndroseIf",
            "ifcTypechecking/WEx1",
            "ifcTypechecking/Wallet_lock_exception",
            "examples/ERC20",
    })
    void testPositive(String contractName) {
        File logDir = new File("./.scif");
        logDir.mkdirs();
        String inputFilePath = contractName + ".scif";
        URL input = ClassLoader.getSystemResource(inputFilePath);
        System.out.println(inputFilePath + ": " + input);
        File NTCConsFile = new File(logDir, "ntc.cons");
        ArrayList<File> files = new ArrayList<>();
        files.add(new File(input.getFile()));

        List<SourceFile> roots = null;
        try {
            roots = TypeChecker.regularTypecheck(files, NTCConsFile, m_debug);
        } catch (Exception e) {
            e.printStackTrace();
        }
        assertNotNull(roots);
    }

    @ParameterizedTest
    @ValueSource(strings = {
            "regularTypechecking/ExceptionThrowAndCatch_W01",
            "regularTypechecking/ExceptionThrowAndCatch_W02",
            "regularTypechecking/ExceptionThrowAndCatch_W03",
            // "regularTypechecking/LocalTrust_W01",
            "regularTypechecking/FinalVarNotInitialized_W01",
            "regularTypechecking/FinalVarNotInitialized_W02",
    })
    void testNegative(String contractName) {
        File logDir = new File("./.scif");
        logDir.mkdirs();
        String inputFilePath = contractName + ".scif";
        URL input = ClassLoader.getSystemResource(inputFilePath);
        System.out.println(inputFilePath + ": " + input);
        File NTCConsFile = new File(logDir, "ntc.cons");
        ArrayList<File> files = new ArrayList<>();
        files.add(new File(input.getFile()));
        try {
            List<SourceFile> roots = TypeChecker.regularTypecheck(files, NTCConsFile, m_debug);
            assertNull(roots);
        } catch (Exception exp) {
            exp.printStackTrace();
        }
    }
}

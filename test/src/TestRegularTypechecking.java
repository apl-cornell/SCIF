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
            "applications/Uniswap_ERC20_noe",
            "applications/Uniswap_ERC777_noe",
            "applications/EthCrossChainManager",
            "applications/SysEscrow",
            "applications/HODLWallet",
            "applications/ERC20_raw",
            "applications/ERC20_nodepmap",
            "applications/ERC20_depmap",
            "applications/Dexible_raw",
            "applications/KoET_raw",
            "applications/ERC777",
//            "applications/Uniswap_ERC20_raw",
            "applications/Uniswap_ERC20",
            "applications/Uniswap_ERC777",
            "basic/StructEx01",
            "basic/DependentMap",
            "basic/EmptyContract",
            "basic/EmptyContract2",
            "basic/ExceptionThrowAndCatch",
            "basic/FinalVar",
            "basic/EndroseIf",
            "ifcTypechecking/WEx1",
            "ifcTypechecking/Wallet_lock_exception",
            "examples/ERC20",
            "examples/DeployToken",
    })
    void testPositive(String contractName) {
        File logDir = new File("./.scif");
        logDir.mkdirs();
        String inputFilePath = contractName + ".scif";
        URL input = ClassLoader.getSystemResource(inputFilePath);
        System.out.println(inputFilePath + ": " + input);
//        File NTCConsFile = new File(logDir, "ntc.cons");
        ArrayList<File> files = new ArrayList<>();
        files.add(new File(input.getFile()));

        List<SourceFile> roots = null;
        try {
            roots = TypeChecker.regularTypecheck(files, logDir, m_debug);
        } catch (Exception e) {
            e.printStackTrace();
        }
        assertNotNull(roots);
    }

    @ParameterizedTest
    @ValueSource(strings = {
            "basic/StructEx_W01",
            "basic/Assignment_W01",
            "regularTypechecking/ExceptionThrowAndCatch_W01",
            "regularTypechecking/ExceptionThrowAndCatch_W02",
            "regularTypechecking/ExceptionThrowAndCatch_W03",
            // "regularTypechecking/LocalTrust_W01",
            "regularTypechecking/FinalVarNotInitialized_W01",
            "regularTypechecking/FinalVarNotInitialized_W02",
            "regularTypechecking/Constructor1",
            "regularTypechecking/Constructor2",
            "regularTypechecking/Constructor3",
//            "regularTypechecking/Constructor4",
    })
    void testNegative(String contractName) {
        File logDir = new File("./.scif");
        logDir.mkdirs();
        String inputFilePath = contractName + ".scif";
        URL input = ClassLoader.getSystemResource(inputFilePath);
        System.out.println(inputFilePath + ": " + input);
//        File NTCConsFile = new File(logDir, "ntc.cons");
        ArrayList<File> files = new ArrayList<>();
        files.add(new File(input.getFile()));
        List<SourceFile> roots = null;
        try {
            roots = TypeChecker.regularTypecheck(files, logDir, m_debug);
        } catch (Exception exp) {
            exp.printStackTrace();
        } catch (AssertionError err) {
            err.printStackTrace();
        }
        if (roots != null) {
            assert false;
        }
        assertNull(roots);
    }
}

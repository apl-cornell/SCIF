import java.io.File;
import java.net.URISyntaxException;
import java.net.URL;
import java_cup.runtime.Symbol;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import parser.*;

import static org.junit.jupiter.api.Assertions.*;

public class TestParsing {

    /** Closure syntax must parse; a SyntaxError fails the test. */
    @Test
    void testClosureBasicParsesStrict() throws Exception {
        URL input = ClassLoader.getSystemResource("parsing/ClosureBasic.scif");
        assertNotNull(input, "parsing/ClosureBasic.scif must be on the test classpath");
        Symbol result = Parser.parse(new File(input.toURI()), null);
        assertNotNull(result, "closure syntax must parse to a non-null AST");
    }

    @ParameterizedTest
    @ValueSource(strings = {
            "applications/EthCrossChainManager",
            "applications/SysEscrow",
            "applications/HODLWallet",
            "basic/EmptyContract",
            "basic/EmptyContract2",
            "basic/DependentMap",
            "basic/StateVarDeclaration",
            "basic/ExceptionDefinition",
            "basic/MethodDefinition",
            "basic/BaseContract",
            "basic/ExceptionThrowAndCatch",
            "basic/EndroseIf",
            "basic/ILockManager",
            "basic/ITrustManager",
            "regularTypechecking/LocalTrust_W01",
            "regularTypechecking/FinalVarNotInitialized_W01",
            "regularTypechecking/FinalVarNotInitialized_W02",
            "regularTypechecking/Constructor1",
            "regularTypechecking/Constructor2",
            "regularTypechecking/Constructor3",
            "regularTypechecking/Constructor4",
            "ifcTypechecking/LocalTrust",
            "ifcTypechecking/Wallet_lock_exception",
            "builtin_files/Contract",
            "builtin_files/ContractImp",
            "builtin_files/ManagedContract",
            "builtin_files/ManagedContractImp",
            "builtin_files/ExternallyManagedContract",
            "builtin_files/ExternallyManagedContractImp",
            "builtin_files/LockManager",
            "builtin_files/TrustManager",
            // "examples/ERC20",
            "examples/IERC20",
            "examples/IExchange",
            "examples/Dexible",
            "examples/Uniswap_W0",
            "examples/KoET",
            "examples/DeployToken",
            "applications/ERC20_raw",
            "applications/ERC20_nodepmap",
            "applications/ERC20_depmap",
            "applications/ERC777",
            "applications/Uniswap_ERC20_raw",
            "applications/Uniswap_ERC777",
            "applications/IERC20",
            "applications/IExchange",
            "applications/Dexible_raw",
            "applications/KoET_raw",
            "parsing/ClosureMultiHole",
            "parsing/ClosureAllBound",
            "parsing/ClosureNoOuterLabel",
    })
    void testPositive(String contractName) throws Exception {
        String inputFilePath = contractName + ".scif";
        URL input = ClassLoader.getSystemResource(inputFilePath);
        assertNotNull(input, inputFilePath + " must be on the test classpath");
        // A SyntaxError propagates and fails the test.
        Symbol result = Parser.parse(new File(input.toURI()), null);
        assertNotNull(result, "expected " + contractName + " to parse");
    }

    @ParameterizedTest
    @ValueSource(strings = {
            "WrongStateVarDeclaration",
            "ClosureHoleInInvoke",
    })
    void testNegative(String contractName) throws Exception {
        String inputFilePath = "parsing/" + contractName + ".scif";
        URL input = ClassLoader.getSystemResource(inputFilePath);
        assertNotNull(input, inputFilePath + " must be on the test classpath");
        boolean parsed;
        try {
            parsed = Parser.parse(new File(input.toURI()), null) != null;
        } catch (Parser.SyntaxError expected) {
            parsed = false;
        }
        assertFalse(parsed, "expected " + contractName + " to fail parsing");
    }
}

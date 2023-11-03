import java.io.File;
import java.net.URISyntaxException;
import java.net.URL;
import java_cup.runtime.Symbol;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import static org.junit.jupiter.api.Assertions.*;

public class TestParsing {

    @ParameterizedTest
    @ValueSource(strings = {
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
            "examples/ERC20",
            "examples/IERC20",
            "examples/IExchange",
            "examples/Dexible",
            "examples/Uniswap_W0",
            "examples/KoET",
    })
    void testPositive(String contractName) {
        //if (!contractName.equals("basic/DependentMap")) return;
        String inputFilePath = contractName + ".scif";
        URL input = ClassLoader.getSystemResource(inputFilePath);
        try {
            Symbol result = Parser.parse(new File(input.toURI()), null);
            assertNotNull(result);
        } catch (URISyntaxException e) {
            System.err.println("IOException when converting the input file URL to URI");
        } /*catch (Exception e) {

        }*/
    }

    @ParameterizedTest
    @ValueSource(strings = {
            "WrongStateVarDeclaration",
    })
    void testNegative(String contractName) {
        String inputFilePath = "parsing/" + contractName + ".scif";
        URL input = ClassLoader.getSystemResource(inputFilePath);
        try {
            Symbol result = Parser.parse(new File(input.toURI()), null);
            assertNull(result);
        } catch (URISyntaxException e) {
            System.err.println("IOException when converting the input file URL to URI");
        } /*catch (Exception e) {

        }*/
    }
}

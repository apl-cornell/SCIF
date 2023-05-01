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
            "basic/DependentMap",
            "basic/StateVarDeclaration",
            "basic/ExceptionDefinition",
            "basic/MethodDefinition",
            "basic/BaseContract",
            "basic/ExceptionThrowAndCatch",
            "basic/EndroseIf",
            "regularTypechecking/LocalTrust_W01",
            "regularTypechecking/FinalVarNotInitialized_W01",
            "regularTypechecking/FinalVarNotInitialized_W02",
            "ifcTypechecking/LocalTrust",
            "ifcTypechecking/Wallet_lock_exception",
            // "examples/ERC20",
    })
    void testPositive(String contractName) {
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

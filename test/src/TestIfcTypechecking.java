import static org.junit.jupiter.api.Assertions.assertNotNull;

import java.io.File;
import java.net.URISyntaxException;
import java.net.URL;
import java_cup.runtime.Symbol;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

public class TestIfcTypechecking {

    @ParameterizedTest
    @ValueSource(strings = {
            "basic/EmptyContract",
            "ifcTypechecking/Wallet_lock_exception",
    })
    void testPositiveIfcTypechecking(String contractName) {
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
}

import java.io.File;
import java.net.URISyntaxException;
import java.net.URL;
import java_cup.runtime.Symbol;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

public class TestParsing {

    @ParameterizedTest
    @ValueSource(strings = {
            "EmptyContract",
            "StateVarDeclaration",
            "ExceptionDefinition",
            "MethodDefinition",
    })
    void testPositiveParsing(String contractName) {
        String inputFilePath = "parsing/" + contractName + ".scif";
        URL input = ClassLoader.getSystemResource(inputFilePath);
        try {
            Symbol result = Parser.parse(new File(input.toURI()), null);
        } catch (URISyntaxException e) {
            System.err.println("IOException when converting the input file URL to URI");
        } /*catch (Exception e) {

        }*/
    }

    void testNegativeParsing() {

    }
}

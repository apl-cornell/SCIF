import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java_cup.runtime.Symbol;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.junit.Test.None;

public class TestParsing {

    @ParameterizedTest
    @ValueSource(strings = {
            "EmptyContract",
            "StateVarDeclaration",
            "ExceptionDefinition",
            // "MethodDefinition",
    })
    void testParsing(String contractName) {
        String inputFilePath = "parsing/" + contractName + ".scif";
        URL input = ClassLoader.getSystemResource(inputFilePath);
        try {
            Symbol result = Parser.parse(new File(input.toURI()), null);
        } catch (URISyntaxException e) {
            System.err.println("IOException when converting the input file URL to URI");
        } /*catch (Exception e) {

        }*/
    }
}

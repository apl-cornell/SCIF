import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

//import ast.SourceFile;
//import java.io.File;
//import java.net.URISyntaxException;
//import java.net.URL;
//import java.util.ArrayList;
//import java.util.List;
//import java_cup.runtime.Symbol;
//import org.junit.jupiter.api.AfterAll;
//import org.junit.jupiter.api.MethodOrderer;
//import org.junit.jupiter.api.Order;
//import org.junit.jupiter.api.Test;
//import org.junit.jupiter.api.TestInstance;
//import org.junit.jupiter.api.TestInstance.Lifecycle;
//import org.junit.jupiter.api.TestMethodOrder;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import typecheck.Utils;

//@TestMethodOrder(MethodOrderer.MethodName.class)
//@TestInstance(Lifecycle.PER_METHOD)
public class TestConstraints {
    boolean m_debug = true;

    @ParameterizedTest
    @ValueSource(strings = {
            "removeLiquidity0.ifc",
            "constructor0.ifc",
    })
    void testPositive(String constraintName) throws Exception {
        sherrloc.diagnostic.DiagnosticConstraintResult result = Utils.runSherrloc("./.scif/" + constraintName);
        System.err.println("runSLC: " + constraintName + " " + result.success());
        assertTrue(result.success());
    }

//    @Test
//    void testBundle() throws Exception {
//        String[] filenames = {
//                "constructor0.ifc",
//                "removeLiquidity0.ifc"
//        };
//        for (String filename: filenames) {
//            sherrloc.diagnostic.DiagnosticConstraintResult result = Utils.runSherrloc("./.scif/" + filename);
//            System.err.println("runSLC: " + filename + " " + result.success());
//            assertTrue(result.success());
//        }
//    }

    @Test
    void test2() throws Exception {
        String[] filenames = {
                "constructor0.ifc",
        };
        for (String filename: filenames) {
            sherrloc.diagnostic.DiagnosticConstraintResult result = Utils.runSherrloc("./.scif/" + filename);
            System.err.println("runSLC: " + filename + " " + result.success());
            assertTrue(result.success());
        }
    }

    @Test
    void test1() throws Exception {
        String[] filenames = {
                "removeLiquidity0.ifc"
        };
        for (String filename: filenames) {
            sherrloc.diagnostic.DiagnosticConstraintResult result = Utils.runSherrloc("./.scif/" + filename);
            System.err.println("runSLC: " + filename + " " + result.success());
            assertTrue(result.success());
        }
    }
}

import static org.junit.jupiter.api.Assertions.*;

import ast.SourceFile;
import java.io.File;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import java_cup.runtime.Symbol;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import parser.Parser;
import typecheck.exceptions.SemanticException;

public class TestRegularTypechecking {
    private boolean m_debug = true;

    @ParameterizedTest
    @ValueSource(strings = {
            "applications/IExchange",
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
            "regularTypechecking/ClosureRegularOK",
            "regularTypechecking/ClosureRegularOK2",
            "regularTypechecking/ClosureImplementsOK",
            "regularTypechecking/ClosureNoOuterLabelOK",
            "regularTypechecking/ClosureJoinPcEx",
            "regularTypechecking/ClosureBinderRegularOK",
    })
    void testPositive(String contractName) throws Exception {
        File logDir = new File("./.scif");
        logDir.mkdirs();
        String inputFilePath = contractName + ".scif";
        URL input = ClassLoader.getSystemResource(inputFilePath);
        System.out.println(inputFilePath + ": " + input);
        ArrayList<File> files = new ArrayList<>();
        files.add(new File(input.getFile()));

        List<SourceFile> roots = Preprocessor.preprocess(files);
        assertNotNull(roots, contractName);
        assertTrue(TypeChecker.regularTypecheck(roots, m_debug),
                "expected " + contractName + " to regular-typecheck");
    }

    @ParameterizedTest
    @ValueSource(strings = {
            "basic/StructEx_W01",
//            "basic/Assignment_W01",
            "regularTypechecking/ExceptionThrowAndCatch_W01",
            "regularTypechecking/ExceptionThrowAndCatch_W02",
            "regularTypechecking/ExceptionThrowAndCatch_W03",
            // "regularTypechecking/LocalTrust_W01",
            "regularTypechecking/FinalVarNotInitialized_W01",
            "regularTypechecking/FinalVarNotInitialized_W02",
            "regularTypechecking/Constructor1",
            "regularTypechecking/Constructor2",
            "regularTypechecking/Constructor3",
            "regularTypechecking/ClosureNotClosurable",
            "regularTypechecking/ClosureInvokeArity",
            "regularTypechecking/ClosureCreateArity",
            "regularTypechecking/ClosureInvokeNonClosure",
            "regularTypechecking/ClosureBindNonClosure",
            "regularTypechecking/ClosureImplementsParamMismatch",
            "regularTypechecking/ClosureLabelNonPrincipal",
            "regularTypechecking/ClosureMeetPcEx",
            "regularTypechecking/ClosureTargetWrite",
//            "regularTypechecking/Constructor4",
    })
    void testNegative(String contractName) {
        File logDir = new File("./.scif");
        logDir.mkdirs();
        String inputFilePath = contractName + ".scif";
        URL input = ClassLoader.getSystemResource(inputFilePath);
        System.out.println(inputFilePath + ": " + input);
        ArrayList<File> files = new ArrayList<>();
        files.add(new File(input.getFile()));
        boolean accepted;
        try {
            List<SourceFile> roots = Preprocessor.preprocess(files);
            accepted = roots != null && TypeChecker.regularTypecheck(roots, m_debug);
        } catch (Throwable t) {
            // Any error means the bad input was correctly not accepted.
            accepted = false;
        }
        assertFalse(accepted, "expected " + contractName + " to be rejected by regular typecheck");
    }

    /**
     * Each contract must be rejected with a reported error: a SemanticException or
     * a failed constraint check. Any other exception fails the test.
     */
    @ParameterizedTest
    @ValueSource(strings = {
            "regularTypechecking/AttributeOfUndefined_W01",
            "regularTypechecking/AttributeNoMember_W01",
            "regularTypechecking/EmitArity_W01",
            "regularTypechecking/ThrowArity_W01",
            "regularTypechecking/TrustQueryNonFinal_W01",
            "regularTypechecking/MethodAsValue_W01",
            "regularTypechecking/CatchUnknownException_W01",
            "regularTypechecking/CallReceiverUndefined_W01",
            "regularTypechecking/CallReceiverNotVariable_W01",
            "regularTypechecking/DeleteNonLvalue_W01",
            "regularTypechecking/PublicMeetExternalPc_W01",
            "regularTypechecking/PublicJoinInternalPc_W01",
            "regularTypechecking/ImplementsMissingMethod_W01",
            "regularTypechecking/CallOptionUnsupported_W01",
            "regularTypechecking/ThrowsUnknownException_W01",
            "regularTypechecking/EmptyFile_W01",
            "regularTypechecking/DepMapNonFinalIndex_W01",
            "regularTypechecking/ThrowUndeclared_W01",
            "regularTypechecking/SubscriptNonCollection_W01",
            "regularTypechecking/LabelNonPrincipalVar_W01",
    })
    void testReportedError(String contractName) throws Exception {
        URL input = ClassLoader.getSystemResource(contractName + ".scif");
        List<File> files = List.of(new File(input.getFile()));
        boolean accepted;
        try {
            List<SourceFile> roots = Preprocessor.preprocess(files);
            accepted = roots != null && TypeChecker.regularTypecheck(roots, m_debug);
        } catch (SemanticException e) {
            accepted = false;
        }
        assertFalse(accepted, "expected " + contractName + " to be rejected by regular typecheck");
    }
}

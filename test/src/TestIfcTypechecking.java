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

public class TestIfcTypechecking {
    boolean m_debug = true;

    @ParameterizedTest
    @ValueSource(strings = {
            "applications/Uniswap_ERC20_noe",
            "applications/Uniswap_ERC777_noe",
            "applications/EthCrossChainManager",
            "applications/SysEscrow",
            "basic/Seq",
            "applications/HODLWallet",
            "applications/Dexible",
            "applications/Uniswap_ERC20",
            "applications/ERC20_depmap",
            "applications/KoET",
            "basic/StructEx01",
            "ifcTypechecking/Wallet_lock_exception",
            "applications/ERC20_nodepmap",
            "basic/DependentMap",
            "basic/EmptyContract",
            "basic/EmptyContract2",
            "basic/ExceptionThrowAndCatch",
            "basic/EndroseIf",
            "examples/SimpleStorage",
            // "examples/ERC20",
            "examples/DeployToken",
            "ifcTypechecking/ClosureCreateIFC",
            "ifcTypechecking/ClosureInvokeIFC",
            "ifcTypechecking/ClosureFieldStoreInvokeIFC",
            "ifcTypechecking/ClosureCrossContractBothThis",
            "ifcTypechecking/ClosureMultiHopInvoker",
            "ifcTypechecking/ClosureReturnIFC",
            "ifcTypechecking/ClosureCreateAsArgIFC",
            "ifcTypechecking/ClosureReturnReceiveIFC",
            "ifcTypechecking/IRouterClosure",
            "ifcTypechecking/ClosureBinderIFC",
            "ifcTypechecking/ClosureBindIFC",
            "ifcTypechecking/ClosureTargetMember",
            "ifcTypechecking/ClosureInvokePostPcAutoendorse",
            "ifcTypechecking/ClosurePcExStronger",
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
        System.out.println("\nInformation Flow Typechecking:");
        assertTrue(TypeChecker.ifcTypecheck(roots, m_debug),
                "expected " + contractName + " to IFC-typecheck");
    }
    @ParameterizedTest
    @ValueSource(strings = {
            "basic/StructEx_W02",
            "applications/Dexible_raw",
            "applications/KoET_raw",
            "ifcTypechecking/DependentMap_W01",
//            "applications/Uniswap_ERC20_raw",
            "applications/ERC20_raw",
            "ifcTypechecking/WEx1",
//            "ifcTypechecking/LocalTrust_W01",
            "ifcTypechecking/Wallet_lock_exception_W01",
            "ifcTypechecking/Wallet_lock_exception_W02",
            "ifcTypechecking/TailCall",
            "ifcTypechecking/ClosureCreateArgLow_W01",
            "ifcTypechecking/ClosureInvokeArgLow_W01",
            "ifcTypechecking/ClosureCreatePcLow_W01",
            "ifcTypechecking/ClosureCreateValLabelHigh_W01",
            "ifcTypechecking/ClosureInvokePcLow_W01",
            "ifcTypechecking/ClosureCreateRecvLow_W01",
            "ifcTypechecking/ClosureInvokeArgPcLow_W01",
            "ifcTypechecking/CallPcLow_W01",
            "ifcTypechecking/CallRecvLow_W01",
            "ifcTypechecking/ClosureMultiHopNoEndorse_W01",
            "ifcTypechecking/ClosureMultiHopWithEndorse",
            "ifcTypechecking/ClosureDeclaredWeakerThanFunc_W01",
            "ifcTypechecking/ClosureReturnDeclaredWeaker_W01",
            "ifcTypechecking/ClosureBinderNotPrincipal_W01",
            "ifcTypechecking/ClosureBinderBoundDependent_W01",
            "ifcTypechecking/ClosureBindArgLow_W01",
            "ifcTypechecking/ClosureBindBoundDependent_W01",
            // "examples/ERC20",
    })
    void testNegative(String contractName) throws Exception {
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
                "expected " + contractName + " to clear regular typecheck "
                        + "(IFC must be the rejector)");
        boolean ifcAccepted;
        try {
            ifcAccepted = TypeChecker.ifcTypecheck(roots, m_debug);
        } catch (typecheck.exceptions.SemanticException e) {
            ifcAccepted = false;
        }
        assertFalse(ifcAccepted,
                "expected " + contractName + " to be rejected by IFC typecheck");
    }
}

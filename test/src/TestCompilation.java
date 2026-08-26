import static org.junit.jupiter.api.Assertions.*;

import ast.SourceFile;
import java.io.File;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

public class TestCompilation {
    boolean m_debug = true;

    @ParameterizedTest
    @ValueSource(strings = {
//            "applications/Uniswap_ERC20_noe",
//            "applications/Uniswap_ERC777_noe",
            "basic/StructEx04",
            "basic/StructEx03",
            "basic/StructEx02",
            "basic/StructEx01",
            "basic/ClosureFieldSmoke",
            "basic/ClosurableEntrySmoke",
            "basic/ClosureCreateSmoke",
            "basic/ClosureInvokeArgsSmoke",
            "basic/ClosureInvokeBinderSmoke",
            "basic/ClosureDynArgSmoke",
            "basic/ClosureBindSmoke",
            "basic/ClosureTargetSmoke",
            "basic/IRouterClosureCompile",
            "applications/EthCrossChainManager",
            "applications/HODLWallet",
            "applications/SysEscrow",
//            "applications/Uniswap_ERC20",
//            "applications/Uniswap_ERC777",
            "applications/Dexible",
            "applications/KoET",
            "applications/ERC20_depmap",
            "applications/ERC20_nodepmap",
            "applications/ERC20_depmap_noe",
//            "examples/DeployToken02", TODO
            "basic/DependentMap",
            "basic/EmptyContract",
            "basic/EmptyContract2",
            "basic/ExceptionThrowAndCatch",
            "basic/EndroseIf",
            "ifcTypechecking/Wallet_lock_exception",
            // "examples/ERC20",
            "examples/SimpleStorage",
            "examples/DeployToken",
            "multiContract/importTest/import1",
            "multiContract/DexibleWithEvents",
    })
    void testPositive(String contractName) throws Exception {
        File logDir = new File("./.scif");
        logDir.mkdirs();
        String inputFilePath = contractName + ".scif";
        URL input = ClassLoader.getSystemResource(inputFilePath);
        System.out.println(inputFilePath + ": " + input);
        List<File> files = new ArrayList<>();
        files.add(new File(input.getFile()));

        List<SourceFile> roots = Preprocessor.preprocess(files);
        assertNotNull(roots, contractName);
        assertTrue(TypeChecker.regularTypecheck(roots, m_debug),
                "expected " + contractName + " to regular-typecheck");
        System.out.println("\nInformation Flow Typechecking:");
        assertTrue(TypeChecker.ifcTypecheck(roots, m_debug),
                "expected " + contractName + " to IFC-typecheck");

        List<SourceFile> fileRoots = new ArrayList<>();
        for (SourceFile r : roots) {
            if (r.getSourceFilePath().equals(input.getPath())) {
                fileRoots.add(r);
            }
        }
        assertFalse(fileRoots.isEmpty(), "root not found for " + input.getPath());

        File outputFile = File.createTempFile("tmp", "sol");
        outputFile.deleteOnExit();
        SolCompiler.compile(fileRoots, outputFile);
        String dump = System.getenv("SCIF_SOL_DUMP_DIR");
        if (dump != null) {
            java.nio.file.Files.copy(outputFile.toPath(),
                    java.nio.file.Path.of(dump,
                            contractName.replace('/', '_') + ".sol"),
                    java.nio.file.StandardCopyOption.REPLACE_EXISTING);
        }
    }

    /**
     * Shapes that typecheck but have no code generation yet must stop
     * with an explicit error, rather than emit Solidity that only solc
     * will reject.
     */
    @ParameterizedTest
    @ValueSource(strings = {
            "basic/ClosureNonVoidGated",
    })
    void testUnsupportedCodegen(String contractName) throws Exception {
        String inputFilePath = contractName + ".scif";
        URL input = ClassLoader.getSystemResource(inputFilePath);
        List<File> files = new ArrayList<>();
        files.add(new File(input.getFile()));

        List<SourceFile> roots = Preprocessor.preprocess(files);
        assertNotNull(roots, contractName);
        assertTrue(TypeChecker.regularTypecheck(roots, m_debug),
                "expected " + contractName + " to regular-typecheck");
        assertTrue(TypeChecker.ifcTypecheck(roots, m_debug),
                "expected " + contractName + " to IFC-typecheck");

        List<SourceFile> fileRoots = new ArrayList<>();
        for (SourceFile r : roots) {
            if (r.getSourceFilePath().equals(input.getPath())) {
                fileRoots.add(r);
            }
        }
        File outputFile = File.createTempFile("tmp", "sol");
        outputFile.deleteOnExit();
        assertThrows(UnsupportedOperationException.class,
                () -> SolCompiler.compile(fileRoots, outputFile),
                "expected " + contractName + " to be rejected by code generation");
    }
}

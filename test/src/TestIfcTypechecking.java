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
            "ifcTypechecking/ClosureBinderUnnamed",
            "ifcTypechecking/ClosureBindIFC",
            "ifcTypechecking/ClosureTargetMember",
            "ifcTypechecking/ClosureInvokePostPcAutoendorse",
            "ifcTypechecking/ClosurePcExStronger",
            "ifcTypechecking/ClosureLockWeaken",
            "ifcTypechecking/SenderTrustedAddress",
            "ifcTypechecking/ValueNowTrusted",
            "ifcTypechecking/TrustFactKept",
            "ifcTypechecking/FinalFieldInit",
            "ifcTypechecking/InterfaceLocalPrincipal",
            "ifcTypechecking/EndorseLabels",
            "ifcTypechecking/EmitIFC",
            "ifcTypechecking/JoinLabelPrincipal",
            "ifcTypechecking/CallTrustedReference",
            "ifcTypechecking/TrustTestTwoBranches",
            "ifcTypechecking/NewTail",
            "ifcTypechecking/NewEndorseTail",
            "ifcTypechecking/NewLocked",
            "ifcTypechecking/CallResultTrusted",
            "ifcTypechecking/CallPcTrusted",
            "ifcTypechecking/ExceptionHandlerTrusted",
            "ifcTypechecking/ExceptionThrowUntrusted",
            "ifcTypechecking/ExceptionData",
            "ifcTypechecking/ExceptionExternalTrusted",
            "ifcTypechecking/ExceptionDependentLabel",
            "ifcTypechecking/WhileBodyPcTrusted",
            "ifcTypechecking/EndorseIfBodyPcTrusted",
            "ifcTypechecking/EndorseIfThrowUntrusted",
            "ifcTypechecking/IfEarlierThrowUntrusted",
            "ifcTypechecking/WhileThrowUntrusted",
            "ifcTypechecking/HandlerThrowUntrusted",
            "ifcTypechecking/RescueAllUntrusted",
            "ifcTypechecking/AssertFalseHandler",
            "ifcTypechecking/StructMemberInput",
            "ifcTypechecking/StructMemberRead",
            "ifcTypechecking/StructMemberWrite",
            "ifcTypechecking/StructForeignMember",
            "ifcTypechecking/StructNestedRead",
            "ifcTypechecking/StructNestedWrite",
            "ifcTypechecking/StructElementRead",
            "ifcTypechecking/StructElementWrite",
            "ifcTypechecking/NestedMapWrite",
            "basic/StructEx04",
            "ifcTypechecking/MapIndexRead",
            "ifcTypechecking/MapIndexWrite",
            "ifcTypechecking/DepMapKeyRead",
            "ifcTypechecking/DepMapUnnamedKeyRead",
            "ifcTypechecking/DepMapKeyWrite",
            "ifcTypechecking/CallElementReceiver",
            "ifcTypechecking/CallMemberReceiver",
            "ifcTypechecking/MemberArrayPush",
            "ifcTypechecking/UncheckedEarlierThrowUntrusted",
            "ifcTypechecking/UncheckedEmpty",
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
            "ifcTypechecking/ClosureBinderUnnamedLow_W01",
            "ifcTypechecking/ClosureBinderBoundDependent_W01",
            "ifcTypechecking/ClosureBindArgLow_W01",
            "ifcTypechecking/ClosureBindBoundDependent_W01",
            "ifcTypechecking/ClosureLockOverclaim_W01",
            "ifcTypechecking/TrustFactReassign_W01",
            "ifcTypechecking/TrustFactBranch_W01",
            "ifcTypechecking/TrustFactLoop_W01",
            "ifcTypechecking/TrustFactFieldCall_W01",
            "ifcTypechecking/NonFinalPrincipalLabel_W01",
            "ifcTypechecking/InterfaceLocalPrincipal_W01",
            "ifcTypechecking/EndorseFromLabel_W01",
            "ifcTypechecking/EndorseToLabel_W01",
            "ifcTypechecking/EndorsePcLow_W01",
            "ifcTypechecking/EmitArgLow_W01",
            "ifcTypechecking/EmitPcLow_W01",
            "ifcTypechecking/EmitDependentLow_W01",
            "ifcTypechecking/EmitUnnamedPrincipal_W01",
            "ifcTypechecking/ThrowUnnamedPrincipal_W01",
            "ifcTypechecking/CallUnnamedPrincipal_W01",
            "ifcTypechecking/CallResultUnnamedPrincipal_W01",
            "ifcTypechecking/JoinLabelPrincipal_W01",
            "ifcTypechecking/CastUnknownContract_W01",
            "ifcTypechecking/CallLendsAuthority_W01",
            "ifcTypechecking/ClosureCrossContractUntrustedSink_W01",
            "ifcTypechecking/TrustTestAfterBranch_W01",
            "ifcTypechecking/TrustTestBeforeBranch_W01",
            "ifcTypechecking/TrustTestTwoBranches_W01",
            "ifcTypechecking/NewUntrusted_W01",
            "ifcTypechecking/NewNonTail_W01",
            "ifcTypechecking/CallResultUntrusted_W01",
            "ifcTypechecking/CallPcUntrusted_W01",
            "ifcTypechecking/ExceptionHandlerLow_W01",
            "ifcTypechecking/ExceptionThrowLow_W01",
            "ifcTypechecking/ExceptionDataLow_W01",
            "ifcTypechecking/ExceptionExternalUntrusted_W01",
            "ifcTypechecking/ExceptionDependentLabelLow_W01",
            "ifcTypechecking/ExceptionDependentThrowLow_W01",
            "ifcTypechecking/WhileBodyPcLow_W01",
            "ifcTypechecking/EndorseIfBodyPcLow_W01",
            "ifcTypechecking/EndorseIfThrowLow_W01",
            "ifcTypechecking/IfEarlierThrowLow_W01",
            "ifcTypechecking/WhileThrowLow_W01",
            "ifcTypechecking/HandlerThrowLow_W01",
            "ifcTypechecking/RescueAllLow_W01",
            "ifcTypechecking/StructMemberInputLow_W01",
            "ifcTypechecking/StructMemberReadLow_W01",
            "ifcTypechecking/StructMemberWriteLow_W01",
            "ifcTypechecking/StructForeignMemberLow_W01",
            "ifcTypechecking/StructNestedReadLow_W01",
            "ifcTypechecking/StructNestedWriteLow_W01",
            "ifcTypechecking/StructElementReadLow_W01",
            "ifcTypechecking/StructElementWriteLow_W01",
            "ifcTypechecking/NestedMapWriteLow_W01",
            "ifcTypechecking/MapIndexReadLow_W01",
            "ifcTypechecking/MapIndexWriteLow_W01",
            "ifcTypechecking/DepMapKeyReadLow_W01",
            "ifcTypechecking/DepMapUnnamedKeyReadLow_W01",
            "ifcTypechecking/DepMapUnnamedKeyWriteLow_W01",
            "ifcTypechecking/CallElementReceiverLow_W01",
            "ifcTypechecking/CallMemberReceiverLow_W01",
            "ifcTypechecking/MemberArrayPushLow_W01",
            "ifcTypechecking/UncheckedEarlierThrowLow_W01",
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

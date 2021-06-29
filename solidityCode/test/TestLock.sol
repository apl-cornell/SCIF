pragma solidity ^0.8.3;

import "truffle/Assert.sol";
import "../contracts/BaseContractCentralized.sol";
import "../contracts/TrustOracle.sol";
import "../contracts/LockOracle.sol";

// test cases are run in order
contract TestLock {
    TrustOracle trustOracle;
    LockOracle lockOracle;
    BaseContractCentralized alice;
    BaseContractCentralized bob;
    BaseContractCentralized carl;
    BaseContractCentralized dan;


    function testLockThenUnlock() public {
        trustOracle = new TrustOracle();
        lockOracle = new LockOracle();

        alice = new BaseContractCentralized(address(trustOracle), address(lockOracle));

        Assert.isTrue(alice.lock(address(alice)), "A contract can lock itself.");
        Assert.isTrue(alice.unlock(address(alice)), "A contract can unlock itself.");
    }

    function testDoubleLock() public {
        bob = new BaseContractCentralized(address(trustOracle), address(lockOracle));

        Assert.isTrue(bob.lock(address(bob)), "A contract can lock itself.");
        Assert.isFalse(bob.lock(address(bob)), "A contract can not double-lock itself.");
    }

    function testUnlockBeforeLock() public {
        carl = new BaseContractCentralized(address(trustOracle), address(lockOracle));

        Assert.isFalse(carl.unlock(address(carl)), "A contract can not unlock itself before any locking.");
    }


}

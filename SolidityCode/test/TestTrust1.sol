pragma solidity ^0.8.3;

import "truffle/Assert.sol";
import "../contracts/BaseContractCentralized.sol";
import "../contracts/TrustOracle.sol";
import "../contracts/LockOracle.sol";

// test cases are run in order
contract TestTrust1 {
    TrustOracle trustOracle;
    LockOracle lockOracle;
    BaseContractCentralized alice;
    BaseContractCentralized bob;
    BaseContractCentralized carl;


    function testInitialization() public {
        trustOracle = new TrustOracle();
        lockOracle = new LockOracle();

        alice = new BaseContractCentralized(address(trustOracle), address(lockOracle));

        Assert.isTrue(alice.ifTrust(address(alice), address(alice)), "A contract by default should trusts itself.");
    }

    function testSetTrust() public {
        bob = new BaseContractCentralized(address(trustOracle), address(lockOracle));
        alice.setTrust(address(bob));

        Assert.isTrue(alice.ifTrust(address(alice), address(bob)), "setTrust should influence the result of ifTrust.");
    }

    function testTransitivity() public {
        carl = new BaseContractCentralized(address(trustOracle), address(lockOracle));
        bob.setTrust(address(carl));

        Assert.isTrue(alice.ifTrust(address(alice), address(carl)), "Trust relationship should be transitive.");
    }

    function testRevokeTrust() public {
        alice.revokeTrust(address(bob));

        Assert.isFalse(alice.ifTrust(address(alice), address(bob)), "Revoke should take effect.");
        Assert.isFalse(alice.ifTrust(address(alice), address(carl)), "Transitive relation should also be revoked.");
    }
}

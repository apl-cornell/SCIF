// SPDX-License-Identifier: MIT
pragma solidity >=0.8.3 <0.9.0;

import "./BaseContract.sol";
import "./TrustOracle.sol";
import "./LockOracle.sol";

contract BaseContractCentralized is BaseContract {
    TrustOracle trustOracle;
    LockOracle lockOracle;

    constructor(address trustOracleAddr, address lockOracleAddr) {
        trustOracle = TrustOracle(trustOracleAddr);
        lockOracle = LockOracle(lockOracleAddr);
    }

    function ifTrust(address a, address b) 
        override
        public
        returns (bool) {
        if (a == address(this) && ifDTrust(b)) {
            return true;
        }
        return trustOracle.ifTrust(a, b);
    }

    function ifDTrust(address trustee)
        override
        public
        view
        returns (bool) {
        return (trusteeIndex[trustee] != 0);
    }

    function getDTrustList()
        override
        public
        view
        returns (address[] memory) {
        return trustees;
    }

    function setTrust(address trustee) override public {
        if (ifDTrust(trustee)) {
            return;
        }
        super.setLocalTrust(trustee);
        trustOracle.setTrust(trustee);
    }

    function provokeTrust(address trustee) override public {
        if (!ifDTrust(trustee)) {
            return;
        }
        super.provokeLocalTrust(trustee);
        trustOracle.provokeTrust(trustee);
    }

    function lock(Label calldata l) override public returns (bool) {
        return lockOracle.lock(l);
    }

    function unlock(Label calldata l) override public returns (bool) {
        return lockOracle.unlock(l);
    }
}

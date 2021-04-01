// SPDX-License-Identifier: MIT
pragma solidity >=0.8.3 <0.9.0;

import "./LabelLib.sol";

contract LockOracle {
    Label[] locks;
    function lock(Label calldata l) public returns (bool) {
        if (exists(l))
            return false;
        locks.push(l);
        return true;
    }
    function unlock(Label calldata l) public returns (bool) {
        if (!exists(l))
            return false;
        uint i = 0;
        uint ind = 0;
        while (i < locks.length) {
            if (LabelLib.equals(locks[i], l)) {
                ind = i;
                break;
            }
            i += 1;
        }
        while (ind < locks.length) {
            locks[ind] = locks[ind + 1];
            ind += 1;
        }
        locks.pop();
        return true;
    }

    function exists(Label memory l) private view returns (bool) {
        for (uint i = 0; i < locks.length; ++i) {
            if (LabelLib.equals(locks[i], l))
                return true;
        }
        return false;
    }
}

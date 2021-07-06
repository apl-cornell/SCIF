// SPDX-License-Identifier: MIT
pragma solidity >=0.8.3 <0.9.0;

import "./LabelLib.sol";
import "./BaseContract.sol";

contract LockOracle {
    Label[] locks;
    function lock(Label memory l) public returns (bool) {
        if (exists(l))
            return false;
        locks.push(l);
        return true;
    }

    function lock(address l) public returns (bool) {
        address[] memory x = new address[](1);
        x[0] = l;
        return lock(Label({values : x}));
    }

    function unlock(Label memory l) public returns (bool) {
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
        locks[ind] = locks[locks.length - 1];
        locks.pop();
        return true;
    }

    function unlock(address l) public returns (bool) {
        address[] memory x = new address[](1);
        x[0] = l;
        return unlock(Label({values : x}));
    }

    function exists(Label memory l) private view returns (bool) {
        for (uint i = 0; i < locks.length; ++i) {
            if (LabelLib.equals(locks[i], l))
                return true;
        }
        return false;
    }

    function ifLocked(address l) public returns (bool) {
        // if there is no any locked label l_2 such that l => l_2 or l_2 => l
        BaseContract l_1 = BaseContract(l);

        for (uint i = 0; i < locks.length; ++i) {
            if (l_1.ifTrust(l, locks[i].values[0]) || l_1.ifTrust(locks[i].values[0], l)) {
                return true;
            }
        }
        return false;
    }

    function ifLocked(address l_1, address l_2) public returns (bool) {
        // if there is no locked label l_3 such that l_1 !=> l_2 join l_3
        BaseContract q = BaseContract(l_1);
        if (q.ifTrust(l_2, l_1)) {
            return false;
        }
        for (uint i = 0; i < locks.length; ++i) {
            if (!q.ifTrust(locks[i].values[0], l_1)) {
                return true;
            }
        }
        return false;
    }
}

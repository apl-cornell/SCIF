// SPDX-License-Identifier: MIT
pragma solidity >=0.8.3 <0.9.0;

// enum LabelOps {MEET, JOIN}
struct Label {
    address[] values;
}

library LabelLib {
    function equals(Label memory l1, Label memory l2) internal pure returns (bool) {
        if (l1.values.length != l2.values.length)
            return false;
        for (uint i = 0; i < l1.values.length; ++i) {
            if (l1.values[i] != l2.values[i])
                return false;
        }
        return true;
    }
}

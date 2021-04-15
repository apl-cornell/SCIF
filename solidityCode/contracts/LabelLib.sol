// SPDX-License-Identifier: MIT
pragma solidity >=0.8.3 <0.9.0;

// enum LabelOps {MEET, JOIN}
struct Label {
    JoinLabel[] values;
}

struct JoinLabel {
    address[] values;
}

library LabelLib {
    function equals(Label memory l1, Label memory l2) internal pure returns (bool) {
        return true;
    }
}
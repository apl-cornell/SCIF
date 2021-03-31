// SPDX-License-Identifier: MIT
pragma solidity >=0.8.3 <0.9.0;

contract TrustOracle {
    mapping (address => uint) memberIndex;
    address[] members;

    mapping (address => mapping (address => uint)) trusteeIndex;
    mapping (address => address[]) trustees;

    function ifTrust(address a, address b) public view returns (bool) {
        assert (memberIndex[a] != 0 && memberIndex[b] != 0);
        mapping (uint => mapping (uint => bool)) memory matrix;
        for (uint i = 0; i < members.length; ++i) {
            matrix[i][i] = true;
            for (uint j = 0; j < trustees[members[i]].length; ++j) {
                matrix[i][memberIndex[trustees[members[i]][j]] - 1] = true;
            }
        }
        for (uint k = 0; k < members.length; ++k)
            for (uint i = 0; i < members.length; ++i)
                for (uint j = 0; j < members.length; ++j)
                    if (matrix[i][k] && matrix[k][j])
                        matrix[i][j] = true;

    
        return matrix[memberIndex[a] - 1][memberIndex[b] - 1];
    }

    function setTrust(address trustee) public {
        address sender = msg.sender;
        assert (memberIndex[sender] != 0);
        if (trusteeIndex[sender][trustee] != 0)
            return;
        trustees[sender].push(trustee);
        trusteeIndex[sender][trustee] = trustees[sender].length;
    }

    function provokeTrust(address trustee) public {
        address sender = msg.sender;
        assert (memberIndex[sender] != 0);
        if (trusteeIndex[sender][trustee] == 0)
            return;
        uint i = trusteeIndex[sender][trustee] - 1;
        while (i < trustees[sender].length) {
            trustees[sender][i] = trustees[sender][i + 1];
            i += 1;
        }
        trustees[sender].pop();
        trusteeIndex[sender][trustee] = 0;
    }
}
// SPDX-License-Identifier: MIT
pragma solidity >=0.8.3 <0.9.0;

contract TrustOracle {
    mapping (address => uint) memberIndex;
    address[] members;

    mapping (address => mapping (address => uint)) trusteeIndex;
    mapping (address => address[]) trustees;

    function isMember(address name) internal view returns (bool) {
        return memberIndex[name] != 0;
    }

    function register() public {
        if (isMember(msg.sender))
            return;
        members.push(msg.sender);
        memberIndex[msg.sender] = members.length;
    }

    mapping (uint => mapping (uint => bool)) matrix;

    function ifTrust(address a, address b) public returns (bool) {
        assert (memberIndex[a] != 0 && memberIndex[b] != 0);
        for (uint i = 0; i < members.length; ++i) {
            for (uint j = 0; j < members.length; ++j) {
                matrix[i][j] = false;
            }
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

    function ifTrust(address a, address b, address[] calldata proof) public returns (bool) {
        if (!ifDTrust(a, proof[0]))
            return false;
        for (uint i = 0; i + 1 < proof.length; ++i) {
            if (!ifDTrust(proof[i], proof[i + 1])) {
                return false;
            }
        }
        return ifDTrust(proof[proof.length - 1], b);
    }

    function ifDTrust(address a, address b) private view returns (bool) {
        return trusteeIndex[a][b] != 0;
    }

    function setTrust(address trustee) public {
        address sender = msg.sender;
        assert (memberIndex[sender] != 0);
        if (trusteeIndex[sender][trustee] != 0)
            return;
        trustees[sender].push(trustee);
        trusteeIndex[sender][trustee] = trustees[sender].length;
    }

    function revokeTrust(address trustee) public {
        address sender = msg.sender;
        assert (memberIndex[sender] != 0);
        if (trusteeIndex[sender][trustee] == 0)
            return;
        uint i = trusteeIndex[sender][trustee] - 1;
        address lastTrustee = trustees[sender][trustees[sender].length - 1];
        trustees[sender][i] = lastTrustee;
        trusteeIndex[sender][lastTrustee] = i + 1;
        trustees[sender].pop();
        trusteeIndex[sender][trustee] = 0;
    }
}

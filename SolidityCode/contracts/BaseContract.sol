// SPDX-License-Identifier: MIT
pragma solidity >=0.8.3 <0.9.0;

import "./LabelLib.sol";

abstract contract BaseContract {

    address[] trustees; 
    mapping (address=>uint) trusteeIndex;

    constructor() {

    }

    function setLocalTrust(address trustee) internal {
        trustees.push(trustee);
        trusteeIndex[trustee] = trustees.length;
    }
    function provokeLocalTrust(address trustee) internal {
        uint ind = trusteeIndex[trustee] - 1;
        while (ind < trustees.length) {
            trustees[ind] = trustees[ind + 1];
            ind += 1;
        }
        trustees.pop();
        trusteeIndex[trustee] = 0;
    }

    function ifTrust(address a, address b) public virtual returns (bool);
    function ifDTrust(address trustee) public view virtual returns (bool);
    function getDTrustList() public view virtual returns (address[] memory);

    function setTrust(address trustee) public virtual;
    function provokeTrust(address trustee) public virtual;

    function lock(Label calldata l) public virtual returns (bool);
    function unlock(Label calldata l) public virtual returns (bool);

    
    function ifTrust(Label memory a, Label memory b) 
        private 
        view 
        returns (bool) {
        return false;
        /*if (LabelLib.isPrim(a))
            if (LabelLib.isPrim(b))
                return ifTrust(a.primValue, b.primValue);
            else {
                if (b.op == LabelOps.MEET)
                    return ifTrust(a, b.compValues[0]) || ifTrust(a, b.compValues[1]);
                else 
                    return ifTrust(a, b.compValues[0]) && ifTrust(a, b.compValues[1]);
            }
        else {
            if (a.op == LabelOps.MEET)
                return ifTrust(a.compValues[0], b) && ifTrust(a.compValues[1], b);
            else
                return ifTrust(a.compValues[0], b) || ifTrust(a.compValues[1], b);
        }*/
    }
}

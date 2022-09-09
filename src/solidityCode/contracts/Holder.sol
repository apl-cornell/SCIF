pragma solidity >=0.8.3;
import "./BaseContractCentralized.sol";
contract Holder is BaseContractCentralized {
    constructor (address tO, address lO, address uniswap, address tokenX, address tokenY)
        BaseContractCentralized(tO, lO)
    {
        setTrust(uniswap);
        setTrust(tokenX);
        setTrust(tokenY);
    }
    function alertSend(address name, uint amount)
        public
    {
        assert(!false);
        assert(ifTrust(address(this), msg.sender));
    }
    function alertReceive(address name, uint amount)
        public
    {
        assert(!false);
        assert(ifTrust(address(this), msg.sender));
    }
}

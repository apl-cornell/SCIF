pragma solidity >=0.8.3;
import "./BaseContractCentralized.sol";
import "./Token.sol";
contract Uniswap is BaseContractCentralized {
    Token tX;
    Token tY;
    constructor (address tO, address lO, address tokenX, address tokenY)
        BaseContractCentralized(tO, lO)
    {
        //tX = Token(tokenX);
        //tY = Token(tokenY);
    }
    function exchangeXForY(uint xSold)
        public
        returns (uint)
    {
        assert(!ifLocked(address(this)));
        assert(true);
        address buyer = msg.sender;
        uint tXSold = xSold;
        uint prod = (getBal(tX, address(this)) * getBal(tY, address(this)));
        uint yKept = (prod / (getBal(tX, address(this)) + tXSold));
        uint yBought = (getBal(tY, address(this)) - yKept);
        assert(tX.transfer(buyer, address(this), tXSold));
        assert(tY.transfer(address(this), buyer, yBought));
        return yBought;
    }
    function getBal(Token token, address k)
        public
        returns (uint)
    {
        assert(!ifLocked(address(this)));
        assert(true);
        return token.getBal(k);
    }
}

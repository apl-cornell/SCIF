pragma solidity ^0.8.3;

import "truffle/Assert.sol";

import "../contracts/TrustOracle.sol";
import "../contracts/LockOracle.sol";
import "../contracts/Uniswap.sol";
import "../contracts/Token.sol";
import "../contracts/Holder.sol";

// test cases are run in order
contract TestUniswap {

    event Show (
        uint value
    );

    uint public initialBalance = 20 ether;

    TrustOracle trustOracle;
    LockOracle lockOracle;
    Uniswap uniswap;
    Token tokenX;
    Token tokenY;
    Holder holder;


    function testInit() public {
        trustOracle = new TrustOracle();
        lockOracle = new LockOracle();
        address tO = address(trustOracle);
        address lO = address(lockOracle);

        tokenX = new Token(tO, lO);
        tokenY = new Token(tO, lO);
        uniswap = new Uniswap(tO, lO, address(tokenX), address(tokenY));

    }

}

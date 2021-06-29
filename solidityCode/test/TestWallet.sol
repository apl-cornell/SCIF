pragma solidity ^0.8.3;

import "truffle/Assert.sol";

import "../contracts/TrustOracle.sol";
import "../contracts/LockOracle.sol";
import "../contracts/Wallet.sol";
import "../contracts/WalletUser.sol";
import "../contracts/BadWalletUser.sol";

// test cases are run in order
contract TestWallet {

    event Show (
        uint value
    );

    uint public initialBalance = 20 ether;

    TrustOracle trustOracle;
    LockOracle lockOracle;
    Wallet wallet;
    WalletUser honestUser;
    BadWalletUser badUser;


    function testInit() public {
        trustOracle = new TrustOracle();
        lockOracle = new LockOracle();

        wallet = new Wallet(address(trustOracle), address(lockOracle));

    }

    function testUserInit() public {
        honestUser = new WalletUser(address(wallet));
        badUser = new BadWalletUser(address(wallet));
    }

    function testDeposit() public {

        Assert.isTrue(honestUser.deposit{value: 6 ether}(), "An honest user to deposit 6 ether");
        Assert.isTrue(badUser.deposit{value: 5 ether}(), "A bad user to deposit 5 ether");
        emit Show(address(wallet).balance);
    }

    function testWithdraw() public {
        Assert.isTrue(honestUser.withdraw(1 ether), "An honest user to withdraw 1 ether");
        emit Show(address(wallet).balance);
    }

    function testSteal() public {
        Assert.isTrue(badUser.withdraw(4 ether), "A bad user to steal 8 ethers");
        emit Show(address(wallet).balance);
    }
}

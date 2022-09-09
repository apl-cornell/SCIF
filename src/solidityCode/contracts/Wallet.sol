pragma solidity >=0.8.3;

import "./BaseContractCentralized.sol";

contract Wallet is BaseContractCentralized {
    event Deposit (
        address addr,
        uint balance,
        uint amount
    );

    event Withdraw (
        address addr,
        uint balance,
        uint amount
    );

    mapping(address => uint) balances;

    constructor(address trustOracle, address lockOracle)
        BaseContractCentralized(trustOracle, lockOracle) {
    }

    function withdraw(uint amount)
        public
    {
        emit Withdraw(msg.sender, balances[msg.sender], amount);
        assert(!ifLocked(address(this)));
        assert(true);
        uint gAmount = amount;
        assert(lock(address(this)));
        if ((balances[msg.sender] >= gAmount)) {
            // payable(msg.sender).send(gAmount);
            msg.sender.call{value :gAmount}("");
            unchecked {
                balances[msg.sender] = (balances[msg.sender] - gAmount) ;
            }
        }
        assert(unlock(address(this)));
    }
    function deposit()
        public
        payable
    {
        emit Deposit(msg.sender, balances[msg.sender], msg.value);
        assert(!ifLocked(address(this)));
        assert(true);
        balances[msg.sender] = (balances[msg.sender] + msg.value);
    }
}

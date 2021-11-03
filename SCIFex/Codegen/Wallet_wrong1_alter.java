contract Wallet [this] {
    {{
        BaseContractCentralized
    }}
    map(address, uint){this} balances;

    Wallet constructor{this >> this; this}(address{this} trustOracle, address{this} lockOracle) {
        @BaseContractCentralized(trustOracle, lockOracle);
    }

    @public
    void withdraw{BOT >> this; BOT}(uint{BOT} amount) {
        if (balances[msg.sender] >= amount) {
            balances[msg.sender] = balances[msg.sender] - amount;
            send(msg.sender, amount);
        }
    }

    @public
    @payable
    void deposit{BOT >> this; this}() {
        balances[msg.sender] = balances[msg.sender] + msg.value;
    }
}

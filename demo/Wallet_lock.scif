contract Wallet [this] {
    map(address, uint){this} balances;

    @public
    void withdraw{BOT >> this; this}(uint{BOT} amount) {
        uint{this} gAmount = endorse(amount, BOT->this);

        lock(this) {
            if{this} (balances[msg.sender] >= gAmount) {
                balances[msg.sender] = balances[msg.sender] - gAmount;
                send(msg.sender, gAmount);
            }
        }
    }

    @public
    @payable
    void deposit{BOT >> this; this}() {
        balances[msg.sender] = balances[msg.sender] + msg.value;
    }
}

contract Wallet [this] {
    uint{this} balance;
    map(address, uint){this} balances;

    bool init{BOT >> this; BOT}() {
    }

    void withdraw{BOT >> this; BOT}(address{BOT} sender, uint{BOT} amount) {

        // ... check if this is the sender

        address{this} gSender = endorse(sender, BOT->this);
        uint{this} gAmount = endorse(amount, BOT->this);

        if{this} (balances[gSender] >= gAmount && balance >= gAmount) {
            balances[gSender] = balances[gSender] - gAmount;
            balance = balance - gAmount;
            send(gSender, gAmount);
        }
    }

    bool decBal{this >> this; this}(address user, uint amount) {
        // ... adjust balances
        return true;
    }
}

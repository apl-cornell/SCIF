contract Wallet [this] {
    DistributedBank{this} otherBank;
    uint{this} balance;
    map(address, uint){this} balances;

    Wallet(address _otherBank) {
        otherBank = (DistributedBank) _otherBank;
    }

    void withdraw{BOT >> this; BOT}(address{BOT} sender, uint{BOT} amount) {

        // ... check if this is the sender

        address{this} gSender = endorse(sender, BOT->this);
        uint{this} gAmount = endorse(amount, BOT->this);

        if{this} (balances[gSender] >= gAmount && balance >= gAmount) {
            balances[gSender] = balances[gSender] - gAmount;
            balance = balance - gAmount;
            otherBank.decBal(gSender, gAmount);
            send(gSender, gAmount);
        }
    }

    void decBal{this >> this; this}(address user, uint amount) {
        // ... adjust balances
    }
}

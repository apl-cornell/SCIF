import Holder;

contract Token [this] {
    {{
        BaseContractCentralized
    }}
    map(address, uint){this} balances;
    map(address, bool){this} isAdmin;

    Token constructor(address tO, address lO, address uniswapAddr) {
        @BaseContractCentralized(tO, lO);
        setTrust(uniswapAddr);
    }

    @public
    bool{this} transfer{this >> this; this}(address frm, address to, uint amount) {
        address sender = msg.sender;
        if{high} (frm != sender && isAdmin[frm] != true) {
            return false;
        }
      
        if{high}	 (balances[frm] < amount) {
      	    return false;
        }
      
        balances[frm] = balances[frm] - amount;
        balances[to] = balances[to] + amount;

        Holder _frm = Holder(frm);
        Holder _to = Holder(to);

        lock(this) {
            _frm.alertSend(to, amount);
            _to.alertReceive(frm, amount);
        }
        return true;
    }

    @public
    uint{this} getBal{BOT >> this; this}(address user) {
        return balances[user];
    }
}

contract Holder[this] {
    {{
        BaseContractCentralized
    }}

    Holder constructor{this >> this; this}(address tO, address lO, address uniswap, address tokenX, address tokenY) {
        @BaseContractCentralized(tO, lO);
        setTrust(uniswap);
        setTrust(tokenX);
        setTrust(tokenY);
    }

    @public
    void alertSend{BOT >> this; BOT}(address x, uint amount) {
	// ...
    }

    @public
    void alertReceive{BOT >> this; BOT}(address x, uint amount) {
	// ...
    }
}

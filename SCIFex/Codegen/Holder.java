contract Holder[this] {
    {{
        BaseContractCentralized
    }}

    Holder constructor(address tO, address lO, address uniswap, address tokenX, address tokenY) {
        @BaseContractCentralized(t0, l0);
        setTrust(uniswap);
        setTrust(tokenX);
        setTrust(tokenY);
    }

    @public
    void alertSend{this >> this; this}(address x, uint amount) {
	// ...
    }

    @public
    void alertReceive{this >> this; this}(address x, uint amount) {
	// ...
    }
}

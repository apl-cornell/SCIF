import Token;

contract Uniswap[this] {
    {{
        BaseContractCentralized;
        {@tX = this}
        {@tY = this}
    }}
    Token{this} tX;
    Token{this} tY;

    Uniswap constructor{this >> this; this}(address tO, address lO, address tokenX, address tokenY) {
        @BaseContractCentralized(tO, lO);

        tX = Token(tokenX);
        tY = Token(tokenY);
    }

    @public
    uint{this} exchangeXForY{BOT >> this; this}(uint xSold) {
        address buyer = msg.sender;
        uint tXSold = endorse(xSold, BOT->high);

        uint prod = getBal(tX, address(this)) * getBal(tY, address(this));
        uint yKept = prod / (getBal(tX, address(this)) + tXSold);
        uint yBought = getBal(tY, address(this)) - yKept;

        lock(this) {
            assert tX.transfer(buyer, address(this), tXSold);
            assert tY.transfer(address(this), buyer, yBought);
        }
        return yBought;
    }

    @public
    uint{this} getBal{this >> this; this}(Token token, address k) {
        return token.getBal(k);
    } 
}


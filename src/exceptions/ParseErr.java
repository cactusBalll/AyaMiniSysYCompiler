package exceptions;

import frontend.Token;

public class ParseErr extends Exception{
    private final Token token;

    public ParseErr(Token token) {
        this.token = token;
    }

    @Override
    public String toString() {
        return String.format("unexpected Token %s at line %d", token.toString(), token.getLine());
    }
}

package frontend.ast;

import frontend.Token;

public class Terminator extends TreeNode{
    private final Token token;
    public Terminator(TreeNode parent, Token token) {
        super(parent);
        this.token = token;
    }

    public Token getToken() {
        return token;
    }

    @Override
    public String toString() {
        return token.toString();
    }
}

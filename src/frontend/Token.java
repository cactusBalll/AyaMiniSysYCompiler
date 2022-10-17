package frontend;

public class Token {
    public enum Type{
        IDENFR,
        INTCON,
        STRCON,
        MAINTK,
        CONSTTK,
        INTTK,
        BREAKTK,
        CONTINUETK,
        IFTK,
        ELSETK,
        NOT,
        AND,
        OR,
        WHILETK,
        GETINTTK,
        PRINTFTK,
        RETURNTK,
        PLUS,
        MINU,
        VOIDTK,
        MULT,
        DIV,
        MOD,
        LSS,
        LEQ,
        GRE,
        GEQ,
        EQL,
        NEQ,
        ASSIGN,
        SEMICN,
        COMMA,
        LPARENT,
        RPARENT,
        LBRACK,
        RBRACK,
        LBRACE,
        RBRACE,
    }
    private final Type type;
    private final int line;  // 行号
    private final String text;

    private boolean wrongFormat = false;

    public boolean isWrongFormat() {
        return wrongFormat;
    }

    public void setWrongFormat(boolean wrongFormat) {
        this.wrongFormat = wrongFormat;
    }

    public Type getType() {
        return type;
    }

    public int getLine() {
        return line;
    }

    public String getText() {
        return text;
    }

    public Token(Type type, int line, String text) {
        this.type = type;
        this.line = line;
        this.text = text;
    }

    @Override
    public String toString() {
        return type.toString() + " " + text;
    }
}

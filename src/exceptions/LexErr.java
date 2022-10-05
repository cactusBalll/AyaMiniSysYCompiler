package exceptions;

/**
 * 词法错误，理论上没有词法错误的情况
 */
public class LexErr extends Exception{
    private final int line;
    private final int col;
    private final char c;
    public LexErr(int line, int col, char c) {
        this.line = line;
        this.col = col;
        this.c = c;
    }

    @Override
    public String toString() {
        return String.format("unexpected char at (%d,%d),%c",line,col,c);
    }
}

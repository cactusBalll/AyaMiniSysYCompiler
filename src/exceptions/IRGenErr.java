package exceptions;

public class IRGenErr extends Exception{
    private final int line;
    public IRGenErr(int line) {
        this.line = line;
    }

    @Override
    public String toString() {
        return String.format("IR generation error occurred near line %d.",line);
    }
}

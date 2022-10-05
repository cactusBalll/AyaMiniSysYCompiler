package frontend;

/**
 * 被要求输出的错误
 */
public class RequiredErr{
    private final char identifier;
    private final int line;

    private RequiredErr(char identifier, int line) {
        this.identifier = identifier;
        this.line = line;
    }

    public char getIdentifier() {
        return identifier;
    }

    public int getLine() {
        return line;
    }

    @Override
    public String toString() {
        return String.format("%d %s",line, identifier);
    }

    public static RequiredErr buildIllegalFormatString(int line) {
        return new RequiredErr('a',line);
    }
    public static RequiredErr buildRedefinedName(int line) {
        return new RequiredErr('b', line);
    }
    public static RequiredErr buildUndefinedName(int line) {
        return new RequiredErr('c', line);
    }
    public static RequiredErr buildBadParamNum(int line) {
        return new RequiredErr('d', line);
    }
    public static RequiredErr buildBadParamTy(int line) {
        return new RequiredErr('e', line);
    }
    public static RequiredErr buildShouldRetVoid(int line) {
        return new RequiredErr('f', line);
    }
    public static RequiredErr buildMissingRet(int line) {
        return new RequiredErr('g', line);
    }
    public static RequiredErr buildConstModified(int line) {
        return new RequiredErr('h', line);
    }
    public static RequiredErr buildMissingSemicolon(int line) {
        return new RequiredErr('i', line);
    }
    public static RequiredErr buildMissingRightParen(int line) {
        return new RequiredErr('j', line);
    }
    public static RequiredErr buildMissingRightBracket(int line) {
        return new RequiredErr('k', line);
    }
    public static RequiredErr buildPrintfParamNotMatch(int line) {
        return new RequiredErr('l', line);
    }
    public static RequiredErr buildCtrlOutOfLoop(int line) {
        return  new RequiredErr('m', line);
    }
}

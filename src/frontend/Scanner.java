package frontend;

import exceptions.LexErr;

import java.util.*;

public class Scanner {
    private final String src;
    private final List<Token> tokens = new ArrayList<>();

    private int pos = 0;
    private int line = 1;
    private int col = 1;

    private Map<String, Token.Type> keywords = new HashMap<String, Token.Type>() {{
        put("main", Token.Type.MAINTK);
        put("const", Token.Type.CONSTTK);
        put("int", Token.Type.INTTK);
        put("break", Token.Type.BREAKTK);
        put("continue", Token.Type.CONTINUETK);
        put("if", Token.Type.IFTK);
        put("else", Token.Type.ELSETK);
        put("while", Token.Type.WHILETK);
        put("getint", Token.Type.GETINTTK);
        put("printf", Token.Type.PRINTFTK);
        put("return", Token.Type.RETURNTK);
        put("void", Token.Type.VOIDTK);
    }};

    public Scanner(String src) {
        this.src = src;
    }

    private char next() throws LexErr {
        if (pos >= src.length()) {
            throw new LexErr(line, col, src.charAt(src.length() - 1));
        }
        return src.charAt(pos);
    }

    private boolean isAtEnd() {
        return pos >= src.length();
    }
    private char next(int offset) throws LexErr {
        if (pos + offset >= src.length()) {
            throw new LexErr(line, col, src.charAt(src.length() - 1));
        }
        return src.charAt(pos + offset);
    }

    private void advance() throws LexErr {
        char c = next();
        if (c == '\n') {
            col = 0;
            line++;
        }
        col++;
        pos++;
    }

    private void consume(char c) throws LexErr {
        if (c == next()) {
            advance();
            return;
        }
        throw new LexErr(line, col, next());
    }

    private void addToken(Token.Type type, String text) {
        tokens.add(new Token(type, line, text));
    }

    private boolean isIdentChar(char c) {
        return Character.isAlphabetic(c) || Character.isDigit(c) || c == '_';
    }

    private boolean isIdentBegin(char c) {
        return Character.isAlphabetic(c) || c == '_';
    }

    public List<Token> run() throws LexErr {
        if (!tokens.isEmpty()) {
            return tokens;
        }
        while (pos < src.length()) {
            switch (next()) {
                case '+':
                    addToken(Token.Type.PLUS, "+");
                    advance();
                    break;
                case '-':
                    addToken(Token.Type.MINU, "-");
                    advance();
                    break;
                case '*':
                    addToken(Token.Type.MULT, "*");
                    advance();
                    break;
                case '%':
                    addToken(Token.Type.MOD, "%");
                    advance();
                    break;
                case ';':
                    addToken(Token.Type.SEMICN, ";");
                    advance();
                    break;
                case ',':
                    addToken(Token.Type.COMMA, ",");
                    advance();
                    break;
                case '(':
                    addToken(Token.Type.LPARENT, "(");
                    advance();
                    break;
                case ')':
                    addToken(Token.Type.RPARENT, ")");
                    advance();
                    break;
                case '[':
                    addToken(Token.Type.LBRACK, "[");
                    advance();
                    break;
                case ']':
                    addToken(Token.Type.RBRACK, "]");
                    advance();
                    break;
                case '{':
                    addToken(Token.Type.LBRACE, "{");
                    advance();
                    break;
                case '}':
                    addToken(Token.Type.RBRACE, "}");
                    advance();
                    break;
                default: {
                    char c = next();
                    if (Character.isWhitespace(c)) {
                        advance();
                        // 没有isAtEnd,末尾加空格就寄了
                        while (!isAtEnd() && Character.isSpaceChar(next())) {
                            advance();
                        }
                    } else if (c == '/') {
                        if (next(1) == '*') {
                            // '/*' 多行注释
                            advance();
                            advance();
                            while (!(next() == '*' && next(1) == '/')) {
                                advance();
                            }
                            advance();
                            advance();
                        } else if (next(1) == '/') {
                            // 单行注释
                            advance();
                            advance();
                            while (next() != '\n') {
                                advance();
                            }
                            advance();
                        } else {
                            // 除法
                            addToken(Token.Type.DIV, "/");
                            advance();
                        }
                    } else if (isIdentBegin(c)) {
                        StringBuffer buffer = new StringBuffer();
                        buffer.append(c);
                        advance();
                        while (isIdentChar(next())) {
                            buffer.append(next());
                            advance();
                        }
                        String ident = buffer.toString();
                        addToken(keywords.getOrDefault(ident, Token.Type.IDENFR), ident);
                    } else if (Character.isDigit(c)) {
                        StringBuffer buffer = new StringBuffer();
                        buffer.append(c);
                        advance();
                        while (Character.isDigit(next())) {
                            buffer.append(next());
                            advance();
                        }
                        // 可能有非法数字，但是并没有这种错误类型
                        String num = buffer.toString();
                        addToken(Token.Type.INTCON, num);
                    } else if (c == '\"') {
                        // todo 格式化字符串可能有非法字符
                        StringBuffer buffer = new StringBuffer();
                        buffer.append(c);
                        advance();
                        while (next() != '\"') {
                            buffer.append(next());
                            advance();
                        }
                        buffer.append(next());
                        int tempLine = line; // 记录当前行
                        advance();
                        addToken(Token.Type.STRCON, buffer.toString());
                        for (int i = 1; i < buffer.length() - 1; i++) { // 注意token是包括前后的引号的，需要去掉
                            char c1 = buffer.charAt(i);
                            if (!isLegalStrConChar(c1)) {
                                if (c1 == '%') {
                                    if (i + 1 >= buffer.length() - 1 || buffer.charAt(i+1) != 'd') {
                                        ErrorHandler.getInstance()
                                                .addError(RequiredErr.buildIllegalFormatString(tempLine));
                                        tokens.get(tokens.size()-1).setWrongFormat(true);
                                    }
                                }
                                if (c1 == '\\') {
                                    if (i + 1 >= buffer.length() - 1 || buffer.charAt(i+1) != 'n') {
                                        ErrorHandler.getInstance()
                                                .addError(RequiredErr.buildIllegalFormatString(tempLine));
                                        tokens.get(tokens.size()-1).setWrongFormat(true);
                                    }
                                }
                            }
                        }

                    } else if (c == '&') {
                        advance();
                        consume('&');
                        addToken(Token.Type.AND, "&&");
                    } else if (c == '|') {
                        advance();
                        consume('|');
                        addToken(Token.Type.OR, "||");
                    } else if (c == '!') {
                        advance();
                        if (next() == '=') {
                            advance();
                            addToken(Token.Type.NEQ, "!=");
                        } else {
                            addToken(Token.Type.NOT, "!");
                        }
                    } else if (c == '<') {
                        advance();
                        if (next() == '=') {
                            advance();
                            addToken(Token.Type.LEQ, "<=");
                        } else {
                            addToken(Token.Type.LSS, "<");
                        }
                    } else if (c == '>') {
                        advance();
                        if (next() == '=') {
                            advance();
                            addToken(Token.Type.GEQ, ">=");
                        } else {
                            addToken(Token.Type.GRE, ">");
                        }
                    } else if (c == '=') {
                        advance();
                        if (next() == '=') {
                            advance();
                            addToken(Token.Type.EQL, "==");
                        } else {
                            addToken(Token.Type.ASSIGN, "=");
                        }
                    } else {
                        throw new LexErr(line, col, next());
                    }

                    break;
                }
            }
        }

        return tokens;
    }

    private boolean isLegalStrConChar(char c) {
        return c == 32 || c == 33 || (c >= 40 && c <= 126 && c !=92);
    }
}

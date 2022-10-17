package frontend;

import exceptions.ParseErr;
import frontend.ast.NonTerminator;
import frontend.ast.Terminator;
import frontend.ast.TreeNode;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class Parser {
    private final List<Token> tokens;
    private final TreeNode root = new NonTerminator(null, NonTerminator.Type.CompUnit, null);

    private int pos = 0;
    private TreeNode current = root;
    public Parser(List<Token> tokens) {
        this.tokens = tokens;
    }
    // 用于回溯
    private int recoverPos = 0;

    private void setRecoverPos() {
        recoverPos = pos;
    }

    private void recover() {
        pos = recoverPos;
    }
    private Token peek(int n) {
        if (pos + n > tokens.size()) {
            return null;
        } else {
            return tokens.get(pos + n);
        }
    }

    private Token peek() throws ParseErr{
        return peekE(0);
    }
    private Token peekE(int n) throws ParseErr {
        if (pos + n > tokens.size()) {
            throw new ParseErr(tokens.get(tokens.size()-1));
        } else {
            return tokens.get(pos + n);
        }
    }

    private void advance() {
        pos++;
    }
    private void advance(int n) {
        pos += n;
    }

    private void consume(Token.Type tokenType) throws ParseErr {
        if (tokens.get(pos).getType() == tokenType) {
            addTerminator(peek());
            pos++;
        } else {
            Token token = tokens.get(pos);
            if (tokenType == Token.Type.SEMICN) {
                ErrorHandler.getInstance()
                        .addError(RequiredErr.buildMissingSemicolon(tokens.get(pos-1).getLine()));
                addTerminator(new Token(Token.Type.SEMICN, -1, ";")); //修复程序
            } else if (tokenType == Token.Type.RBRACK) {
                ErrorHandler.getInstance()
                        .addError(RequiredErr.buildMissingRightBracket(tokens.get(pos-1).getLine()));
                addTerminator(new Token(Token.Type.RBRACK, -1, "]")); //修复程序
            } else if (tokenType == Token.Type.RPARENT) {
                ErrorHandler.getInstance()
                        .addError(RequiredErr.buildMissingRightParen(tokens.get(pos-1).getLine()));
                addTerminator(new Token(Token.Type.RPARENT, -1, ")")); //修复程序
            } else {
                throw new ParseErr(token);
            }
        }
    }

    private void consume() throws ParseErr {
        addTerminator(peek());
        advance();
    }
    private void intoNonTerminator(NonTerminator.Type type) {
        ((NonTerminator)current).addChildAtLast(
                new NonTerminator(current, type,null));
        current = ((NonTerminator)current).getChildAtLast();
    }

    private void outNonTerminator() {
        current = current.getParent();
    }

    private void addTerminator(Token token) {
        ((NonTerminator)current).addChildAtLast(new Terminator(current, token));
    }
    private boolean isInFirstSetOfExp(Token token) {
        return token.getType() == Token.Type.PLUS ||
                token.getType() == Token.Type.MINU ||
                token.getType() == Token.Type.NOT ||
                token.getType() == Token.Type.LPARENT ||
                token.getType() == Token.Type.IDENFR ||
                token.getType() == Token.Type.INTCON;
    }
    public TreeNode run() throws ParseErr{
        compUnit();
        return root;
    }

    private void compUnit() throws ParseErr{
        while (peekE(2).getType() != Token.Type.LPARENT) {
            //intoNonTerminator(NonTerminator.Type.Decl);
            decl();
            //outNonTerminator();
        }
        while (peekE(1).getType() != Token.Type.MAINTK) {
            intoNonTerminator(NonTerminator.Type.FuncDef);
            funcDef();
            outNonTerminator();
        }
        intoNonTerminator(NonTerminator.Type.MainFuncDef);
        mainFuncDef();
        outNonTerminator();
    }
    private void decl() throws ParseErr{
        // decl blockItem BType 不需要出现在树上
        if (peekE(0).getType() == Token.Type.CONSTTK) {
            intoNonTerminator(NonTerminator.Type.ConstDecl);
            constDecl();
            outNonTerminator();
        } else {
            intoNonTerminator(NonTerminator.Type.VarDecl);
            varDecl();
            outNonTerminator();
        }
    }



    private void constDecl() throws ParseErr{
        consume(Token.Type.CONSTTK);
        consume(Token.Type.INTTK);

        intoNonTerminator(NonTerminator.Type.ConstDef);
        constDef();
        outNonTerminator();

        while (peek().getType() == Token.Type.COMMA) {
            consume();
            intoNonTerminator(NonTerminator.Type.ConstDef);
            constDef();
            outNonTerminator();
        }
        consume(Token.Type.SEMICN);
    }

    private void constDef() throws ParseErr{
        consume(Token.Type.IDENFR);
        while (peek().getType() == Token.Type.LBRACK) {
            consume();
            intoNonTerminator(NonTerminator.Type.ConstExp);
            constExp();
            outNonTerminator();
            consume(Token.Type.RBRACK);
        }
        if (peek().getType() == Token.Type.ASSIGN) {
            consume(Token.Type.ASSIGN);
            intoNonTerminator(NonTerminator.Type.ConstInitVal);
            constInitVal();
            outNonTerminator();
        }

    }

    private void constExp() throws ParseErr{
        intoNonTerminator(NonTerminator.Type.AddExp);
        addExp();
        outNonTerminator();
    }

    private void constInitVal() throws ParseErr{
        if (peek().getType() == Token.Type.LBRACE) {
            consume();
            if (peek().getType() != Token.Type.RBRACE) {
                intoNonTerminator(NonTerminator.Type.ConstInitVal);
                constInitVal();
                outNonTerminator();
                while (peek().getType() == Token.Type.COMMA) {
                    consume();
                    intoNonTerminator(NonTerminator.Type.ConstInitVal);
                    constInitVal();
                    outNonTerminator();
                }
            }
            consume(Token.Type.RBRACE);
        } else {
            intoNonTerminator(NonTerminator.Type.ConstExp);
            constExp();
            outNonTerminator();
        }
    }
    private void varDecl() throws ParseErr{
        consume(Token.Type.INTTK);

        intoNonTerminator(NonTerminator.Type.VarDef);
        varDef();
        outNonTerminator();

        while (peek().getType() == Token.Type.COMMA) {
            consume();
            intoNonTerminator(NonTerminator.Type.VarDef);
            varDef();
            outNonTerminator();
        }
        consume(Token.Type.SEMICN);
    }

    private void varDef() throws ParseErr{
        consume(Token.Type.IDENFR);
        while (peek().getType() == Token.Type.LBRACK) {
            consume();
            intoNonTerminator(NonTerminator.Type.ConstExp);
            constExp();
            outNonTerminator();
            consume(Token.Type.RBRACK);
        }
        if (peek().getType() == Token.Type.ASSIGN) {
            consume(Token.Type.ASSIGN);
            intoNonTerminator(NonTerminator.Type.InitVal);
            initVal();
            outNonTerminator();
        }

    }
    private void initVal() throws ParseErr{
        if (peek().getType() == Token.Type.LBRACE) {
            consume();
            if (peek().getType() != Token.Type.RBRACE) {
                intoNonTerminator(NonTerminator.Type.InitVal);
                initVal();
                outNonTerminator();
                while (peek().getType() == Token.Type.COMMA) {
                    consume();
                    intoNonTerminator(NonTerminator.Type.InitVal);
                    initVal();
                    outNonTerminator();
                }
            }
            consume(Token.Type.RBRACE);
        } else {
            intoNonTerminator(NonTerminator.Type.Exp);
            exp();
            outNonTerminator();
        }
    }
    private void funcDef() throws ParseErr{
        intoNonTerminator(NonTerminator.Type.FuncType);
        funcType();
        outNonTerminator();
        consume(Token.Type.IDENFR);
        consume(Token.Type.LPARENT);
        if (peek().getType() == Token.Type.INTTK)  {
            intoNonTerminator(NonTerminator.Type.FuncFParams);
            funcFParams();
            outNonTerminator();
        }
        consume(Token.Type.RPARENT);
        intoNonTerminator(NonTerminator.Type.Block);
        block();
        outNonTerminator();
    }

    private void mainFuncDef() throws ParseErr{
        consume(Token.Type.INTTK);
        consume(Token.Type.MAINTK);
        consume(Token.Type.LPARENT);
        consume(Token.Type.RPARENT);
        intoNonTerminator(NonTerminator.Type.Block);
        block();
        outNonTerminator();
    }

    private void funcType() throws ParseErr{
        consume();
    }

    private void funcFParams() throws ParseErr{
        intoNonTerminator(NonTerminator.Type.FuncFParam);
        funcFParam();
        outNonTerminator();
        while (peek().getType() == Token.Type.COMMA) {
            consume();
            intoNonTerminator(NonTerminator.Type.FuncFParam);
            funcFParam();
            outNonTerminator();
        }
    }

    private void funcFParam() throws ParseErr {
        consume(Token.Type.INTTK);
        consume(Token.Type.IDENFR);
        // 数组最多只有二维
        if (peek().getType() == Token.Type.LBRACK) {
            consume();
            consume(Token.Type.RBRACK);
            if (peek().getType() == Token.Type.LBRACK) {
                consume();
                intoNonTerminator(NonTerminator.Type.ConstExp);
                constExp();
                outNonTerminator();
                consume(Token.Type.RBRACK);
            }
        }

    }
    private void block() throws ParseErr{
        consume(Token.Type.LBRACE);
        while (peek().getType() != Token.Type.RBRACE) {
            if (peek().getType() == Token.Type.INTTK ||
                    peek().getType() == Token.Type.CONSTTK) {
                decl();
            } else {
                intoNonTerminator(NonTerminator.Type.Stmt);
                stmt();
                outNonTerminator();
            }
        }
        consume(Token.Type.RBRACE);
    }

    private void stmt() throws ParseErr{
        if (peek().getType() == Token.Type.IFTK) {
            consume();
            consume(Token.Type.LPARENT);
            intoNonTerminator(NonTerminator.Type.Cond);
            cond();
            outNonTerminator();
            consume(Token.Type.RPARENT);
            intoNonTerminator(NonTerminator.Type.Stmt);
            stmt();
            outNonTerminator();
            if (peek().getType() == Token.Type.ELSETK) {
                consume();
                intoNonTerminator(NonTerminator.Type.Stmt);
                stmt();
                outNonTerminator();
            }
            ((NonTerminator)current).setInnerType(NonTerminator.InnerType.IfStmt);
        } else if (peek().getType() == Token.Type.WHILETK) {
            consume();
            consume(Token.Type.LPARENT);
            intoNonTerminator(NonTerminator.Type.Cond);
            cond();
            outNonTerminator();
            consume(Token.Type.RPARENT);

            intoNonTerminator(NonTerminator.Type.Stmt);
            stmt();
            outNonTerminator();
            ((NonTerminator)current).setInnerType(NonTerminator.InnerType.WhileStmt);
        } else if (peek().getType() == Token.Type.BREAKTK) {
            consume();
            consume(Token.Type.SEMICN);
            ((NonTerminator)current).setInnerType(NonTerminator.InnerType.BreakStmt);
        } else if (peek().getType() == Token.Type.CONTINUETK) {
            consume();
            consume(Token.Type.SEMICN);
            ((NonTerminator)current).setInnerType(NonTerminator.InnerType.ContinueStmt);
        } else if (peek().getType() == Token.Type.RETURNTK) {
            consume();
            if (isInFirstSetOfExp(peek())) {
               intoNonTerminator(NonTerminator.Type.Exp);
               exp();
               outNonTerminator();
            }
            consume(Token.Type.SEMICN);
            ((NonTerminator)current).setInnerType(NonTerminator.InnerType.ReturnStmt);
        } else if (peek().getType() == Token.Type.PRINTFTK) {
            consume();
            consume(Token.Type.LPARENT);
            consume(Token.Type.STRCON);
            while (peek().getType() == Token.Type.COMMA) {
                consume();
                intoNonTerminator(NonTerminator.Type.Exp);
                exp();
                outNonTerminator();
            }
            consume(Token.Type.RPARENT);
            consume(Token.Type.SEMICN);
            ((NonTerminator)current).setInnerType(NonTerminator.InnerType.PrintfStmt);
        } else if (peek().getType() == Token.Type.LBRACE) {
            intoNonTerminator(NonTerminator.Type.Block);
            block();
            outNonTerminator();
            ((NonTerminator)current).setInnerType(NonTerminator.InnerType.BlockStmt);
        } else if (peek().getType() == Token.Type.SEMICN) {
            consume();
            ((NonTerminator)current).setInnerType(NonTerminator.InnerType.EptStmt);
        } else {
            // 处理最左符号都是LVal的情况
            if (peek().getType() == Token.Type.IDENFR) {
                setRecoverPos();
                intoNonTerminator(NonTerminator.Type.LVal);
                lVal();
                outNonTerminator();
                if (peek().getType() == Token.Type.ASSIGN) {
                    if (peek(1).getType() == Token.Type.GETINTTK) {
                        consume();
                        consume();
                        consume(Token.Type.LPARENT);
                        consume(Token.Type.RPARENT);
                        ((NonTerminator)current).setInnerType(NonTerminator.InnerType.InputStmt);
                    } else {
                        consume();
                        intoNonTerminator(NonTerminator.Type.Exp);
                        exp();
                        outNonTerminator();
                        ((NonTerminator)current).setInnerType(NonTerminator.InnerType.AssignStmt);
                    }
                } else {
                    ((NonTerminator)current).removeChildAtLast();
                    recover();
                    intoNonTerminator(NonTerminator.Type.Exp);
                    exp();
                    outNonTerminator();
                    ((NonTerminator)current).setInnerType(NonTerminator.InnerType.ExpStmt);
                }
            } else {
                intoNonTerminator(NonTerminator.Type.Exp);
                exp();
                outNonTerminator();
                ((NonTerminator)current).setInnerType(NonTerminator.InnerType.ExpStmt);
            }
            consume(Token.Type.SEMICN);
        }
    }


    private void cond() throws  ParseErr {
        intoNonTerminator(NonTerminator.Type.LOrExp);
        lOrExp();
        outNonTerminator();
    }

    //表达式分析部分
    private void lOrExp() throws ParseErr{
        intoNonTerminator(NonTerminator.Type.LAndExp);
        lAndExp();
        outNonTerminator();
        while (peek().getType() == Token.Type.OR) {
            consume();
            intoNonTerminator(NonTerminator.Type.LAndExp);
            lAndExp();
            outNonTerminator();
        }
        expReform(NonTerminator.Type.LOrExp);
    }
    // 把表达式树转化为要求的形式
    private void expReform(NonTerminator.Type type) {
        // 表达式树里面还混了终结符
        List<TreeNode> nodeList = ((NonTerminator)current).getChildren();
        if (nodeList.size() > 1) {
            NonTerminator t = new NonTerminator(null, type, null);
            //lhs
            nodeList.get(0).setParent(t);
            t.addChildAtLast(nodeList.get(0));
            //op
            for (int i = 1; i < nodeList.size(); i+=2) {
                NonTerminator t2 = new NonTerminator(null, type, null);
                t.setParent(t2);
                t2.addChildAtLast(t);

                //op
                nodeList.get(i).setParent(t2);
                t2.addChildAtLast(nodeList.get(i));

                //rhs
                nodeList.get(i+1).setParent(t2);
                t2.addChildAtLast(nodeList.get(i+1));

                t = t2;
            }
            //rhs
            //nodeList.clear();
            //t.setParent(current);
            //((NonTerminator) current).addChildAtLast(t);
            t.setParent(current.getParent());
            // replacing ...
            ((NonTerminator)t.getParent()).removeChildAtLast();
            ((NonTerminator)t.getParent()).addChildAtLast(t);
            current = t;
        }
    }

    // 对于二元表达式而言，结构都是一样的。。
    // 然而Java没有宏
    private void lAndExp() throws ParseErr{
        intoNonTerminator(NonTerminator.Type.EqExp);
        eqExp();
        outNonTerminator();
        while (peek().getType() == Token.Type.AND) {
            consume();
            intoNonTerminator(NonTerminator.Type.EqExp);
            eqExp();
            outNonTerminator();
        }
        expReform(NonTerminator.Type.LAndExp);
    }

    private void eqExp() throws ParseErr{
        intoNonTerminator(NonTerminator.Type.RelExp);
        relExp();
        outNonTerminator();
        while (peek().getType() == Token.Type.EQL ||
                peek().getType() == Token.Type.NEQ) {
            consume();
            intoNonTerminator(NonTerminator.Type.RelExp);
            relExp();
            outNonTerminator();
        }
        expReform(NonTerminator.Type.EqExp);
    }

    private void relExp() throws ParseErr{
        intoNonTerminator(NonTerminator.Type.AddExp);
        addExp();
        outNonTerminator();
        while (peek().getType() == Token.Type.LSS ||
                peek().getType() == Token.Type.LEQ ||
                peek().getType() == Token.Type.GRE ||
                peek().getType() == Token.Type.GEQ) {
            consume();
            intoNonTerminator(NonTerminator.Type.AddExp);
            addExp();
            outNonTerminator();
        }
        expReform(NonTerminator.Type.RelExp);
    }

    private void addExp() throws ParseErr{
        intoNonTerminator(NonTerminator.Type.MulExp);
        mulExp();
        outNonTerminator();
        while (peek().getType() == Token.Type.PLUS ||
                peek().getType() == Token.Type.MINU) {
            consume();
            intoNonTerminator(NonTerminator.Type.MulExp);
            mulExp();
            outNonTerminator();
        }
        expReform(NonTerminator.Type.AddExp);
    }

    private void mulExp() throws ParseErr{
        intoNonTerminator(NonTerminator.Type.UnaryExp);
        unaryExp();
        outNonTerminator();
        while (peek().getType() == Token.Type.MULT ||
                peek().getType() == Token.Type.DIV ||
                peek().getType() == Token.Type.MOD) {
            consume();
            intoNonTerminator(NonTerminator.Type.UnaryExp);
            unaryExp();
            outNonTerminator();
        }
        expReform(NonTerminator.Type.MulExp);
    }

    private void unaryExp() throws ParseErr{
        if (peek().getType() == Token.Type.PLUS ||
            peek().getType() == Token.Type.MINU ||
            peek().getType() == Token.Type.NOT) {
            intoNonTerminator(NonTerminator.Type.UnaryOp);
            unaryOp();
            outNonTerminator();
            intoNonTerminator(NonTerminator.Type.UnaryExp);
            unaryExp();
            outNonTerminator();
        } else {
            if (peek().getType() == Token.Type.IDENFR) {
                if (peek(1).getType() == Token.Type.LPARENT) {
                    consume();
                    consume();
                    //todo 检查右括号是否闭合
                    if (isInFirstSetOfExp(peek())) {
                        intoNonTerminator(NonTerminator.Type.FuncRParams);
                        funcRParams();
                        outNonTerminator();
                    }
                    consume(Token.Type.RPARENT);
                } else {
                    intoNonTerminator(NonTerminator.Type.PrimaryExp);
                    primaryExp();
                    outNonTerminator();
                }
            } else {
                intoNonTerminator(NonTerminator.Type.PrimaryExp);
                primaryExp();
                outNonTerminator();
            }
        }
    }

    private void primaryExp() throws ParseErr{
        if (peek().getType() == Token.Type.LPARENT) {
            consume();
            intoNonTerminator(NonTerminator.Type.Exp);
            exp();
            outNonTerminator();
            consume(Token.Type.RPARENT);
        } else if (peek().getType() == Token.Type.IDENFR) {
            intoNonTerminator(NonTerminator.Type.LVal);
            lVal();
            outNonTerminator();
        } else {
            intoNonTerminator(NonTerminator.Type.Number);
            number();
            outNonTerminator();
        }
    }

    private void funcRParams() throws ParseErr{
        intoNonTerminator(NonTerminator.Type.Exp);
        exp();
        outNonTerminator();
        while (peek().getType() == Token.Type.COMMA) {
            consume();
            intoNonTerminator(NonTerminator.Type.Exp);
            exp();
            outNonTerminator();
        }
    }

    private void unaryOp() throws ParseErr{
        consume(); // 只有一个用法，调用方已经检查过符号了
    }
    private void lVal() throws ParseErr{
        consume(Token.Type.IDENFR);
        while (peek().getType() == Token.Type.LBRACK) {
            consume();
            intoNonTerminator(NonTerminator.Type.Exp);
            exp();
            outNonTerminator();
            consume(Token.Type.RBRACK);
        }
    }

    private void number() throws ParseErr{
        consume(Token.Type.INTCON);
    }

    private void exp() throws ParseErr{
        intoNonTerminator(NonTerminator.Type.AddExp);
        addExp();
        outNonTerminator();
    }

}

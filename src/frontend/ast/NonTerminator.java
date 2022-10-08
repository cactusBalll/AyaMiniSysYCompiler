package frontend.ast;

import java.util.ArrayList;
import java.util.List;

public class NonTerminator extends TreeNode{
    private final List<TreeNode> children = new ArrayList<>();
    public enum Type{
        CompUnit,
        Decl,
        ConstDecl,
        BType,
        ConstDef,
        ConstInitVal,
        VarDecl,
        VarDef,
        InitVal,
        FuncDef,
        MainFuncDef,
        FuncType,
        FuncFParams,
        FuncFParam,
        Block,
        BlockItem,
        Stmt,
        Exp,
        Cond,
        LVal,
        PrimaryExp,
        UnaryExp,
        UnaryOp,
        FuncRParams,
        MulExp,
        AddExp,
        RelExp,
        EqExp,
        LAndExp,
        LOrExp,
        ConstExp,
        Number,
    }//要求输出的非终结符

    public enum InnerType{
        IfStmt,
        WhileStmt,
        BreakStmt,
        ContinueStmt,
        ReturnStmt,
        InputStmt,
        PrintfStmt,
        ExpStmt,
        EptStmt,
        AssignStmt,
    }//内部用来区别的类型

    public Type getType() {
        return type;
    }

    public void setType(Type type) {
        this.type = type;
    }

    public InnerType getInnerType() {
        return innerType;
    }

    public void setInnerType(InnerType innerType) {
        this.innerType = innerType;
    }

    private Type type;
    private InnerType innerType;
    public NonTerminator(TreeNode parent,Type type, InnerType innerType) {
        super(parent);
        this.type = type;
        this.innerType = innerType;
    }
    public void addChildAtLast(TreeNode node) {
        children.add(node);
    }

    public TreeNode getChildAtLast() {
        return children.get(children.size()-1);
    }

    public void removeChildAtLast() {
        children.remove(children.size()-1);
    }
    public List<TreeNode> getChildren() {
        return children;
    }

    public TreeNode getChild(int i) {
        return children.get(i);
    }

    public int getChildSize() {
        return children.size();
    }
    @Override
    public String toString() {
        return String.format("<%s>",type);
    }
}

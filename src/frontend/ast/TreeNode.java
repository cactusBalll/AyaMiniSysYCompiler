package frontend.ast;

import frontend.Token;

import java.util.List;
import java.util.function.Function;

public class TreeNode {
    private TreeNode parent;

    public TreeNode getParent() {
        return parent;
    }

    public void setParent(TreeNode parent) {
        this.parent = parent;
    }

    public TreeNode(TreeNode parent) {
        this.parent = parent;
    }

    public static void postOrderPrint(TreeNode current) {
        if (current instanceof NonTerminator) {
            NonTerminator t = (NonTerminator) current;
            for (TreeNode n :
                    t.getChildren()) {
                postOrderPrint(n);
            }
        }
        System.out.println(current);
    }

    /**
     * 调用前需要保证n是个非终结符
     * @param n
     * @param i
     * @return
     */
    public static TreeNode childAt(TreeNode n, int i) {
        return ((NonTerminator)n).getChild(i);
    }

    public static String match(TreeNode n, int i, Token.Type type) {
        TreeNode child = ((NonTerminator)n).getChild(i);
        if (!(child instanceof Terminator)) {
            return null;
        }
        if (((Terminator)child).getToken().getType() == type) {
            return ((Terminator) child).getToken().getText();
        }
        return null;
    }
}

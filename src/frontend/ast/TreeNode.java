package frontend.ast;

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

}

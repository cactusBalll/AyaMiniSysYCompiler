package frontend;

import frontend.ast.TreeNode;
import ir.value.CompUnit;

public class IRGen {
    private final TreeNode root;
    private final CompUnit compUnit = new CompUnit();

    public IRGen(TreeNode root) {
        this.root = root;
    }

    public CompUnit run() {

        return compUnit;
    }
}

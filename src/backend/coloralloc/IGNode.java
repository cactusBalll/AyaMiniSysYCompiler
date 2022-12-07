package backend.coloralloc;

import backend.regs.PReg;
import backend.regs.Reg;

import java.util.*;

/**
 * 冲突图节点
 */
public class IGNode {
    public Reg reg;
    public Set<IGNode> edges = new HashSet<>();
    public int degree = 0;
    public IGNode alias;
    public PReg color;
    public Set<IGMove> moveList = new HashSet<>();

    public boolean loadAsParam = false; // 4个之后的参数

    public IGNode(Reg reg) {
        this.reg = reg;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        IGNode igNode = (IGNode) o;
        return Objects.equals(reg, igNode.reg);
    }

    @Override
    public int hashCode() {
        return Objects.hash(reg);
    }
}

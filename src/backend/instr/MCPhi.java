package backend.instr;

import backend.MCBlock;
import backend.regs.Reg;
import util.Pair;

import java.util.List;

/**
 * 后端代码生成后还需要消除这些phi。
 */
public class MCPhi extends MCInstr{
    public List<Pair<Reg, MCBlock>> pairs;
    public Reg dest;

    public MCPhi(List<Pair<Reg, MCBlock>> pairs, Reg dest) {
        this.pairs = pairs;
        this.dest = dest;
    }
}

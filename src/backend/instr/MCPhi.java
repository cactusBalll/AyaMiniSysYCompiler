package backend.instr;

import backend.MCBlock;
import backend.PsuMCBlock;
import backend.regs.PReg;
import backend.regs.Reg;
import backend.regs.ValueReg;
import ir.instruction.PhiInstr;
import util.Pair;

import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * 后端代码生成后还需要消除这些phi。
 */
public class MCPhi extends MCInstr{
    public List<Pair<Reg, MCBlock>> pairs;
    public Reg dest;

    public static MCPhi fromPhi(PhiInstr phiInstr) {
        return new MCPhi(
                phiInstr.getPhiPairs().stream().map(
                        p -> new Pair<Reg,MCBlock>(new ValueReg(p.getFirst()), new PsuMCBlock(p.getLast()))).
                        collect(Collectors.toList()),
                null
        );
    }
    
    public MCPhi(List<Pair<Reg, MCBlock>> pairs, Reg dest) {
        this.pairs = pairs;
        this.dest = dest;
    }

    @Override
    public Set<Reg> getDef() {
        Set<Reg> ret = new HashSet<>();
        ret.add(dest);
        return ret;
    }

    @Override
    public Set<Reg> getUse() {
        Set<Reg> ret = new HashSet<>();
        for (Pair<Reg, MCBlock> p :
                pairs) {
            ret.add(p.getFirst());
        }
        return ret;
    }

    @Override
    public void allocate(Reg vReg, Reg pReg) {

    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append(dest).append(" = phi");
        for (Pair<Reg, MCBlock> p :
                pairs) {
            sb.append(" [").append(p.getFirst()).append(",").append(p.getLast().label.name).append("],");
        }
        return sb.toString();
    }
}

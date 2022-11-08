package backend.instr;

import backend.regs.PReg;
import backend.regs.Reg;
import util.Pair;

import java.util.ArrayList;
import java.util.List;

public class MCParallelCopy extends MCInstr{
    public List<Pair<Reg, Reg>> copies = new ArrayList<>();

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append("pc[");
        for (Pair<Reg, Reg> pair :
                copies) {
            sb.append(pair.getFirst()).append("<-").append(pair.getLast()).append(',');
        }
        sb.append(']');
        return sb.toString();
    }

    @Override
    public void allocate(Reg vReg, Reg pReg) {

    }
}

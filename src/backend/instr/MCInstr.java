package backend.instr;

import backend.regs.PReg;
import backend.regs.Reg;
import util.MyNode;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public abstract class MCInstr {
    private final MyNode node = new MyNode<>(this);

    public List<Reg> getDef() {
        return new ArrayList<>();
    }
    public List<Reg> getUse() {
        return new ArrayList<>();
    }
    public MyNode getNode() {
        return node;
    }

    public abstract void allocate(Reg vReg, Reg pReg);
}

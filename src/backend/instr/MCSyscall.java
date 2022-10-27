package backend.instr;

import backend.regs.PReg;
import backend.regs.Reg;
import exceptions.BackEndErr;

import java.util.HashSet;
import java.util.Set;

public class MCSyscall extends MCInstr{

    public boolean mayModify = false; // 可能修改v0
    @Override
    public String toString() {
        return "syscall";
    }

    @Override
    public Set<Reg> getDef() {
        Set<Reg> ret = new HashSet<>();
        if (mayModify) {
            try {
                ret.add(PReg.getRegByName("v0"));
            } catch (BackEndErr e) {
                throw new RuntimeException(e);
            }
        }
        return ret;
    }
}

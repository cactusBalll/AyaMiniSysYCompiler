package backend.instr;

import backend.regs.PReg;
import backend.regs.Reg;
import exceptions.BackEndErr;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class MCSyscall extends MCInstr{

    public boolean mayModify = false; // 可能修改v0
    @Override
    public String toString() {
        return "syscall";
    }

    @Override
    public List<Reg> getDef() {
        List<Reg> ret = new ArrayList<>();
        if (mayModify) {
            try {
                ret.add(PReg.getRegByName("v0"));
            } catch (BackEndErr e) {
                throw new RuntimeException(e);
            }
        }
        return ret;
    }

    @Override
    public List<Reg> getUse() {
        List<Reg> ret = new ArrayList<>();
        try {
            ret.add(PReg.getRegByName("v0"));
            if (!mayModify) { //putint & putstr
                ret.add(PReg.getRegByName("a0"));
            }

        } catch (BackEndErr e) {
            throw new RuntimeException(e);
        }
        return ret;
    }

    @Override
    public void allocate(Reg vReg, Reg pReg) {

    }
}

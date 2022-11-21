package backend.instr;

import Driver.AyaConfig;
import backend.regs.PReg;
import backend.regs.Reg;
import backend.regs.VReg;
import util.MyUtils;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * 仅携带def，use信息的空指令
 * 比如函数开头结尾对于被调用者保护寄存器的定义使用
 */
public class MCNop extends MCInstr{
    @Override
    public void allocate(Reg vReg, Reg pReg) {
        if (def.contains(vReg)) {
            def.remove(vReg);
            def.add(pReg);
        }
        if (use.contains(vReg)) {
            use.remove(vReg);
            use.add(pReg);
        }
    }

    public MCNop(Set<Reg> def, Set<Reg> use) {
        this.def = def;
        this.use = use;
    }
    private Set<Reg> def;
    private Set<Reg> use;

    @Override
    public List<Reg> getDef() {
        return new ArrayList<>(def);
    }

    @Override
    public List<Reg> getUse() {
        return new ArrayList<>(use);
    }

    @Override
    public String toString() {
        if (AyaConfig.PRINT_IR_BB_INFO) {
            return "use:"+ MyUtils.formatList(new ArrayList<>(use)) +"def:"+MyUtils.formatList(new ArrayList<>(def));
        } else {
            return "";
        }

    }

}

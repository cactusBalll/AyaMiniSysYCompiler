package backend;

import backend.instr.*;
import backend.regs.PReg;
import backend.regs.Reg;
import backend.regs.VReg;
import backend.regs.ValueReg;
import exceptions.BackEndErr;
import ir.instruction.BinaryOp;
import ir.value.InitVal;
import util.Pair;

public class DivOpt {
    private static final int N = 32;

    private static int calcCLZ(int n) {
        int ret = 0;
        n = n >>> 1;
        while (n != 0) {
            n = n >>> 1;
            ret++;
        }
        return ret;
    }

    public static void genSignedMod(MCBlock mcbb, BinaryOp modInstr, VReg dest) throws BackEndErr {
        assert modInstr.getOpType() == BinaryOp.OpType.Mod;
        assert modInstr.getRight() instanceof InitVal;
        Reg left = new ValueReg(modInstr.getLeft());
        int imm = ((InitVal) modInstr.getRight()).getValue();
        VReg vReg = VReg.alloc();
        VReg vReg1 = VReg.alloc();
        divGen(mcbb, vReg, left, imm);
        MCInstr mcInstr = new MCLi(imm, vReg1);
        mcbb.list.add(mcInstr.getNode());

        mcInstr = new MCInstrR(null, vReg, vReg1, MCInstrR.Type.mult);
        mcbb.list.add(mcInstr.getNode());

        mcInstr = new MCMflo(vReg1);
        mcbb.list.add(mcInstr.getNode());

        mcInstr = new MCInstrR(dest, left, vReg1, MCInstrR.Type.subu);
        mcbb.list.add(mcInstr.getNode());

    }
    public static void genSignedDiv(MCBlock mcbb, BinaryOp divInstr, VReg dest) throws BackEndErr {
        assert divInstr.getRight() instanceof InitVal;
        assert divInstr.getOpType() == BinaryOp.OpType.Div;
        Reg left = new ValueReg(divInstr.getLeft());
        int imm = ((InitVal) divInstr.getRight()).getValue();
        divGen(mcbb, dest, left, imm);
    }

    private static void divGen(MCBlock mcbb, VReg dest, Reg left, int imm) throws BackEndErr {
        int abs = imm > 0 ? imm : -imm;
        assert abs != 0;
        //abs = +1/-1应在前端处理

        VReg vReg;
        if ((abs & (abs - 1)) == 0) {
            int l = calcCLZ(abs);
            VReg v = VReg.alloc();
            MCInstr mcInstr = new MCInstrI(v, left, 31, MCInstrI.Type.sra);
            mcbb.list.add(mcInstr.getNode());

            VReg v1 = VReg.alloc();
            mcInstr = new MCInstrI(v1, v, 32 - l, MCInstrI.Type.srl);
            mcbb.list.add(mcInstr.getNode());

            VReg v2 = VReg.alloc();
            mcInstr = new MCInstrR(v2, left, v1, MCInstrR.Type.addu);
            mcbb.list.add(mcInstr.getNode());

            mcInstr = new MCInstrI(dest, v2, l, MCInstrI.Type.sra);
            mcbb.list.add(mcInstr.getNode());

        } else {
            long nc = ((long) 1 << 31) - (((long) 1 << 31) % abs) - 1;
            long p = 32;
            while (((long) 1 << p) <= nc * (abs - ((long) 1 << p) % abs)) {
                p++;
            }
            long m = ((((long) 1 << p) + (long) abs - ((long) 1 << p) % abs) / (long) abs);
            int n = (int) ((m << 32) >>> 32);
            int shift = (int) (p - 32);

            VReg v = VReg.alloc();
            MCInstr instr = new MCLi(n,v);
            mcbb.list.add(instr.getNode());
            VReg v2 = VReg.alloc();
            if (m >= 2147483648L) {
                VReg v1 = VReg.alloc();
                instr = new MCInstrR(null, left, v, MCInstrR.Type.mult);
                mcbb.list.add(instr.getNode());
                instr = new MCMfhi(v1);
                mcbb.list.add(instr.getNode());
                instr = new MCInstrR(v2, v1, left, MCInstrR.Type.addu);
                mcbb.list.add(instr.getNode());
            } else {
                instr = new MCInstrR(null, left, v, MCInstrR.Type.mult);
                mcbb.list.add(instr.getNode());
                instr = new MCMfhi(v2);
                mcbb.list.add(instr.getNode());
            }
            VReg v3 = VReg.alloc();
            VReg v4 = VReg.alloc();
            instr = new MCInstrI(v3, v2, shift, MCInstrI.Type.sra);
            mcbb.list.add(instr.getNode());

            instr = new MCInstrI(v4, left, 31, MCInstrI.Type.srl);
            mcbb.list.add(instr.getNode());

            instr = new MCInstrR(dest, v3, v4, MCInstrR.Type.addu);
            mcbb.list.add(instr.getNode());

        }

        if (imm < 0) {
            MCInstr instr =
                    new MCInstrR(dest, PReg.getRegById(0), dest, MCInstrR.Type.subu);
            mcbb.list.add(instr.getNode());
        }
    }

}

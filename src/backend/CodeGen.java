package backend;

import Driver.AyaConfig;
import backend.instr.*;
import backend.regs.PReg;
import backend.regs.Reg;
import backend.regs.VReg;
import backend.regs.ValueReg;
import exceptions.BackEndErr;
import ir.instruction.*;
import ir.value.*;
import ty.IntArrTy;
import ty.IntTy;
import util.MyNode;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public class CodeGen {
    private CompUnit compUnit;
    private MCUnit mcUnit = new MCUnit();

    private MCFunction mcFunction;
    private Map<Value, Reg> value2Reg = new HashMap<>();

    private Map<Value, Integer> value2StackArr = new HashMap<>();
    private Map<Value, Label> value2Label = new HashMap<>();

    private Map<BasicBlock, MCBlock> bb2mcbb = new HashMap<>();

    //private Map<Value, Reg> value2RegInFunc = new HashMap<>();

    private Label stack;
    private int stackSlot;

    public CodeGen(CompUnit compUnit) {
        this.compUnit = compUnit;
    }

    public MCUnit run() throws BackEndErr {

        //init stack
        MCSpace space1 = new MCSpace(new Label("stack", null), AyaConfig.STACK_SIZE);
        mcUnit.data.add(space1.getNode());

        for (MyNode<AllocInstr> allocNode :
                compUnit.getGlobalValueList()) {
            AllocInstr alloc = allocNode.getValue();
            Constant init = alloc.getInitVal();
            Label label = new Label(alloc.getName(), null);
            if (init.isZeroInitialized()) {
                assert alloc.getAllocTy() instanceof IntArrTy;
                IntArrTy intArrTy = (IntArrTy) alloc.getAllocTy();
                int size = intArrTy.getDims().size() == 2 ? intArrTy.getDims().get(0) * intArrTy.getDims().get(1) :
                        intArrTy.getDims().get(0);
                MCSpace space = new MCSpace(label, size);
                mcUnit.data.add(space.getNode());

            } else if (init.getListValue() == null) {
                assert alloc.getAllocTy() instanceof IntTy;
                List<Integer> t = new ArrayList<>();
                t.add(init.getValue());
                MCWord space = new MCWord(label, t);
                mcUnit.data.add(space.getNode());
            } else {
                assert alloc.getAllocTy() instanceof IntArrTy;
                // 所有权不明直接clone
                MCWord words = new MCWord(label, new ArrayList<>(init.getListValue()));
                mcUnit.data.add(words.getNode());
            }
            value2Label.put(alloc, label);
        }

        for (MyNode<Function> fNode :
                compUnit.getList()) {
            genFunction(fNode.getValue());
        }
        return mcUnit;
    }

    private MCBlock getBlock() {
        MCBlock block = new MCBlock();
        block.label = Label.getLabel(block);
        return block;
    }

    private int strCnt = 0;
    private Label getStrLabel() {
        Label l = new Label("str"+strCnt, null);
        strCnt++;
        return l;
    }
    private void genFunction(Function f) throws BackEndErr {
        mcFunction = new MCFunction();
        mcUnit.list.add(mcFunction.getNode());
        value2Reg.clear();
        value2StackArr.clear();
        VReg.resetCnt();

        //处理参数
        stackSlot = 0;
        //stackSlot += f.getParams().size(); // 按调用规范，放在寄存器中的参数也要分配栈空间
        stackSlot += 1; // for ra
        MCBlock b = getBlock();
        mcFunction.label = b.label;
        mcFunction.list.add(b.getNode()); //  加入函数入口块
        bb2mcbb.put(f.getFirstBB(), b);

        for (int i = 0; i < Math.min(4, f.getParams().size()); i++) {
            Param p = f.getParams().get(i);
            Reg r = VReg.alloc();
            MCInstr instr = new MCMove(r, PReg.getRegById(4 + i));
            b.list.add(instr.getNode()); // 添加参数寄存器到VReg的move
            // 特殊用途寄存器的liverange要尽量段，防止产生大量spill
            value2Reg.put(p, r);
        }

        if (f.getParams().size() > 4) { // 超出寄存器范围的参数
            for (int i = 4; i < f.getParams().size(); i++) {
                Param p = f.getParams().get(i);
                Reg r = VReg.alloc();
                MCLw instr = new MCLw(r, PReg.getRegByName("sp"), -i * 4);
                b.list.add(instr.getNode());
                value2Reg.put(p, r);
            }
        }
        List<Reg> calleeSaved = new ArrayList<>();
        for (int i = 0; i < 8; i++) {
            Reg r = VReg.alloc();
            Reg pr = PReg.getRegById(8 + i);
            MCMove instr = new MCMove(r, pr);
            b.list.add(instr.getNode());
            calleeSaved.add(r);
        }
        for (MyNode<BasicBlock> bbNode :
                f.getList()) {
            BasicBlock bb = bbNode.getValue();
            if (bb == f.getFirstBB()) {
                genBlock(bb, b, calleeSaved);
            } else {
                MCBlock mcBlock = getBlock();
                bb2mcbb.put(bb, mcBlock);
                genBlock(bb, mcBlock, calleeSaved);
                mcFunction.list.add(mcBlock.getNode());

            }
        }

    }

    /**
     * 从SSA基本块向机器基本块生成代码
     *
     * @param bb   SSA基本块
     * @param mcbb 机器基本块
     */
    public void genBlock(BasicBlock bb, MCBlock mcbb, List<Reg> calleeSaved) throws BackEndErr {
        for (MyNode<Instr> instrNode :
                bb.getList()) {
            Instr instr = instrNode.getValue();
            if (instr instanceof BinaryOp) {
                translateBiOp(mcbb, (BinaryOp) instr);
            } else if (instr instanceof AllocInstr) {
                AllocInstr allocInstr = (AllocInstr) instr;
                IntArrTy intArrTy = (IntArrTy) allocInstr.getAllocTy();
                int slot;
                if (intArrTy.getDims().size() == 2) {
                    slot = intArrTy.getDims().get(0) * intArrTy.getDims().get(1);
                } else {
                    slot = intArrTy.getDims().get(0);
                }
                value2StackArr.put(allocInstr, stackSlot); // 记录数组在栈上的位置
                stackSlot += slot;
            } else if (instr instanceof ArrView) {
                VReg vReg = VReg.alloc();
                MCInstr mcInstr;
                int stackOffset = value2StackArr.get(((ArrView) instr).getArr());
                if (((ArrView) instr).getIdx() instanceof InitVal) {
                    mcInstr = new MCLi(
                            ((InitVal) ((ArrView) instr).getIdx()).getValue() + stackOffset,
                            vReg
                    );
                } else {
                    mcInstr = new MCInstrI(
                            vReg,
                            new ValueReg(((ArrView) instr).getIdx()),
                            stackOffset,
                            MCInstrI.Type.addu
                    );
                }
                mcbb.list.add(mcInstr.getNode());
                value2Reg.put(instr,vReg);
            } else if (instr instanceof BrInstr) {
                MCInstr br = new MCInstrB(
                        MCInstrB.Type.bne,
                        new ValueReg(((BrInstr) instr).getCond()),
                        PReg.getRegById(0),
                        new PsuMCBlock(((BrInstr) instr).getBr0())
                );
                MCInstr j = new MCJ(
                        new PsuMCBlock(((BrInstr) instr).getBr1())
                );
                mcbb.list.add(br.getNode());
                mcbb.list.add(j.getNode());
            } else if (instr instanceof BuiltinCallInstr) {
                if (((BuiltinCallInstr) instr).getFunc() == BuiltinCallInstr.Func.GetInt) {
                    VReg vReg = VReg.alloc();
                    MCInstr mcInstr = new MCLi(
                            5,
                            PReg.getRegByName("v0")
                    );
                    MCInstr syscall = new MCSyscall();
                    mcbb.list.add(mcInstr.getNode());
                    mcbb.list.add(syscall.getNode());
                    value2Reg.put(instr,vReg);
                } else if (((BuiltinCallInstr) instr).getFunc() == BuiltinCallInstr.Func.PutInt) {
                    if (((BuiltinCallInstr) instr).getParam() instanceof InitVal) {
                        MCInstr li = new MCLi(
                                ((InitVal) ((BuiltinCallInstr) instr).getParam()).getValue(),
                                PReg.getRegByName("a0")
                        );
                        mcbb.list.add(li.getNode());
                    } else {
                        MCInstr mov = new MCMove(
                                PReg.getRegByName("a0"),
                                new ValueReg(((BuiltinCallInstr) instr).getParam())
                        );
                        mcbb.list.add(mov.getNode());
                    }

                    MCInstr mcInstr = new MCLi(
                            1,
                            PReg.getRegByName("v0")
                    );
                    MCInstr syscall = new MCSyscall();

                    mcbb.list.add(mcInstr.getNode());
                    mcbb.list.add(syscall.getNode());
                } else if (((BuiltinCallInstr) instr).getFunc() == BuiltinCallInstr.Func.PutStr) {
                    String str1 = ((MyString)((BuiltinCallInstr) instr).getParam()).getStr();
                    MCAsciiz str = new MCAsciiz(getStrLabel(), str1);
                    mcUnit.data.add(str.getNode());
                    MCInstr la = new MCLa(
                            str.label,
                            PReg.getRegByName("a0")
                    );
                    MCInstr mcInstr = new MCLi(
                            4,
                            PReg.getRegByName("v0")
                    );
                    MCInstr syscall = new MCSyscall();
                    mcbb.list.add(la.getNode());
                    mcbb.list.add(mcInstr.getNode());
                    mcbb.list.add(syscall.getNode());
                }
            }
        }
    }

    private void translateBiOp(MCBlock mcbb, BinaryOp instr) throws BackEndErr {
        Value left = instr.getLeft();
        Value right = instr.getRight();
        VReg vReg = VReg.alloc();
        switch (instr.getOpType()) {
            case Add: {
                MCInstr mcInstr = null;
                if (left instanceof InitVal) {
                    mcInstr = new MCInstrI(
                            vReg,
                            new ValueReg(right),
                            ((InitVal) left).getValue(),
                            MCInstrI.Type.addu
                    );
                } else if (right instanceof InitVal) {
                    mcInstr = new MCInstrI(
                            vReg,
                            new ValueReg(left),
                            ((InitVal) right).getValue(),
                            MCInstrI.Type.addu
                    );
                } else {
                    mcInstr = new MCInstrR(
                            vReg,
                            new ValueReg(left),
                            new ValueReg(right),
                            MCInstrR.Type.addu
                    );
                }
                mcbb.list.add(mcInstr.getNode());
                break;
            }
            case Sub: {
                MCInstr mcInstr = null;
                MCInstr mcInstr1 = null;
                Reg t = null;
                if (left instanceof InitVal) { // 没有反向减法指令

                    mcInstr = new MCInstrI(
                            vReg,
                            new ValueReg(right),
                            ((InitVal) left).getValue(),
                            MCInstrI.Type.subu
                    );
                    mcInstr1 = new MCInstrR(
                            vReg,
                            PReg.getRegById(0),
                            vReg,
                            MCInstrR.Type.subu
                    );
                    mcbb.list.add(mcInstr.getNode());
                    mcbb.list.add(mcInstr1.getNode());

                } else if (right instanceof InitVal) {
                    mcInstr = new MCInstrI(
                            vReg,
                            new ValueReg(left),
                            -((InitVal) right).getValue(),
                            MCInstrI.Type.addiu
                    );
                    mcbb.list.add(mcInstr.getNode());
                } else {
                    mcInstr = new MCInstrR(
                            vReg,
                            new ValueReg(left),
                            new ValueReg(right),
                            MCInstrR.Type.subu
                    );
                    mcbb.list.add(mcInstr.getNode());
                }
                break;
            }
            case Mul: { // 其实有一个操作数时常数可以优化，div，mod也是
                MCInstr mcInstr = null;
                MCInstr mcInstr1 = null;
                MCInstr mcInstr2 = null;
                Reg t = PReg.getRegById(1);
                if (left instanceof InitVal) {
                    mcInstr1 = new MCLi(
                            ((InitVal) left).getValue(),
                            t
                    );
                    mcInstr = new MCInstrR(
                            null,
                            t,
                            new ValueReg(right),
                            MCInstrR.Type.mult
                    );
                    mcInstr2 = new MCMflo(vReg);
                    mcbb.list.add(mcInstr1.getNode());
                    mcbb.list.add(mcInstr.getNode());
                    mcbb.list.add(mcInstr2.getNode());
                } else if (right instanceof InitVal) {
                    mcInstr1 = new MCLi(
                            ((InitVal) right).getValue(),
                            t
                    );
                    mcInstr = new MCInstrR(
                            null,
                            new ValueReg(left),
                            t,
                            MCInstrR.Type.mult
                    );
                    mcInstr2 = new MCMflo(vReg);
                    mcbb.list.add(mcInstr1.getNode());
                    mcbb.list.add(mcInstr.getNode());
                    mcbb.list.add(mcInstr2.getNode());
                } else {
                    mcInstr = new MCInstrR(
                            null,
                            new ValueReg(left),
                            new ValueReg(right),
                            MCInstrR.Type.mult
                    );
                    mcInstr2 = new MCMflo(vReg);
                    mcbb.list.add(mcInstr.getNode());
                    mcbb.list.add(mcInstr2.getNode());
                }
                break;
            }
            case Div: {
                MCInstr mcInstr = null;
                MCInstr mcInstr1 = null;
                MCInstr mcInstr2 = null;
                Reg t = PReg.getRegById(1); //at
                if (left instanceof InitVal) {
                    mcInstr1 = new MCLi(
                            ((InitVal) left).getValue(),
                            t
                    );
                    mcInstr = new MCInstrR(
                            null,
                            t,
                            new ValueReg(right),
                            MCInstrR.Type.div
                    );
                    mcInstr2 = new MCMflo(vReg);
                    mcbb.list.add(mcInstr1.getNode());
                    mcbb.list.add(mcInstr.getNode());
                    mcbb.list.add(mcInstr2.getNode());
                } else if (right instanceof InitVal) {
                    mcInstr1 = new MCLi(
                            ((InitVal) right).getValue(),
                            t
                    );
                    mcInstr = new MCInstrR(
                            null,
                            new ValueReg(left),
                            t,
                            MCInstrR.Type.div
                    );
                    mcInstr2 = new MCMflo(vReg);
                    mcbb.list.add(mcInstr1.getNode());
                    mcbb.list.add(mcInstr.getNode());
                    mcbb.list.add(mcInstr2.getNode());
                } else {
                    mcInstr = new MCInstrR(
                            null,
                            new ValueReg(left),
                            new ValueReg(right),
                            MCInstrR.Type.div
                    );
                    mcInstr2 = new MCMflo(vReg);
                    mcbb.list.add(mcInstr.getNode());
                    mcbb.list.add(mcInstr2.getNode());
                }
                break;
            }
            case Mod: {
                MCInstr mcInstr = null;
                MCInstr mcInstr1 = null;
                MCInstr mcInstr2 = null;
                Reg t = PReg.getRegById(1);
                if (left instanceof InitVal) {
                    mcInstr1 = new MCLi(
                            ((InitVal) left).getValue(),
                            t
                    );
                    mcInstr = new MCInstrR(
                            null,
                            t,
                            new ValueReg(right),
                            MCInstrR.Type.div
                    );
                    mcInstr2 = new MCMfhi(vReg);
                    mcbb.list.add(mcInstr1.getNode());
                    mcbb.list.add(mcInstr.getNode());
                    mcbb.list.add(mcInstr2.getNode());
                } else if (right instanceof InitVal) {
                    mcInstr1 = new MCLi(
                            ((InitVal) right).getValue(),
                            t
                    );
                    mcInstr = new MCInstrR(
                            null,
                            new ValueReg(left),
                            t,
                            MCInstrR.Type.div
                    );
                    mcInstr2 = new MCMfhi(vReg);
                    mcbb.list.add(mcInstr1.getNode());
                    mcbb.list.add(mcInstr.getNode());
                    mcbb.list.add(mcInstr2.getNode());
                } else {
                    mcInstr = new MCInstrR(
                            null,
                            new ValueReg(left),
                            new ValueReg(right),
                            MCInstrR.Type.div
                    );
                    mcInstr2 = new MCMfhi(vReg);
                    mcbb.list.add(mcInstr.getNode());
                    mcbb.list.add(mcInstr2.getNode());
                }
                break;
            }
            case Seq:
            case Sne:
            case Sge:
            case Sgt:
            case Sle: // 减少重复代码
            {
                MCInstr mcInstr = null;
                String s = instr.getOpType().toString().toLowerCase(); // IR和MIPS同名
                if (left instanceof InitVal) {
                    mcInstr = new MCInstrI(
                            vReg, new ValueReg(right), ((InitVal) left).getValue(), MCInstrI.Type.valueOf(s));
                } else if (right instanceof InitVal) {
                    mcInstr = new MCInstrI(
                            vReg, new ValueReg(left), ((InitVal) right).getValue(), MCInstrI.Type.valueOf(s));
                } else {
                    mcInstr = new MCInstrR(
                            vReg, new ValueReg(left), new ValueReg(right), MCInstrR.Type.valueOf(s)
                    );
                }
                mcbb.list.add(mcInstr.getNode());
                break;
            }
            case Not: {
                MCInstr mcInstr = new MCInstrI(
                        vReg,
                        new ValueReg(right),
                        1,
                        MCInstrI.Type.xori
                );
                mcbb.list.add(mcInstr.getNode());
                break;
            }
            case Slt: { //slt 没有对应伪指令。。
                MCInstr mcInstr;
                MCInstr mcInstr1;
                if (right instanceof InitVal) {
                    if (((InitVal) right).getValue() < (1<<15 -1)) {
                        mcInstr = new MCInstrI(
                                vReg,
                                new ValueReg(left),
                                ((InitVal) right).getValue(),
                                MCInstrI.Type.slti
                        );
                        mcbb.list.add(mcInstr.getNode());
                    } else {
                        mcInstr1 = new MCLi(
                                ((InitVal) right).getValue(),
                                PReg.getRegById(1)
                        );
                        mcInstr = new MCInstrR(
                                vReg,
                                new ValueReg(left),
                                PReg.getRegById(1),
                                MCInstrR.Type.slt
                        );
                        mcbb.list.add(mcInstr1.getNode());
                        mcbb.list.add(mcInstr.getNode());
                    }
                } else if (left instanceof InitVal) {
                    mcInstr1 = new MCLi(
                            ((InitVal) left).getValue(),
                            PReg.getRegById(1)
                    );
                    mcInstr = new MCInstrR(
                            vReg,
                            PReg.getRegById(1),
                            new ValueReg(right),
                            MCInstrR.Type.slt
                    );
                    mcbb.list.add(mcInstr1.getNode());
                    mcbb.list.add(mcInstr.getNode());
                } else {
                    mcInstr = new MCInstrR(
                            vReg,
                            new ValueReg(left),
                            new ValueReg(right),
                            MCInstrR.Type.slt
                    );
                    mcbb.list.add(mcInstr.getNode());
                }
                break;
            }
        }
        value2Reg.put(instr, vReg);
    }
}

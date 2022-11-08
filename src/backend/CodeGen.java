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
import util.Pair;

import java.util.*;
import java.util.stream.Collectors;

public class CodeGen {
    private CompUnit compUnit;
    private MCUnit mcUnit = new MCUnit();

    private MCFunction mcFunction;
    private Map<Value, Reg> value2Reg = new HashMap<>();

    private Map<Value, Integer> value2StackArr = new HashMap<>();
    private Map<Value, Label> value2Label = new HashMap<>();

    private Map<BasicBlock, MCBlock> bb2mcbb = new HashMap<>();
    private Map<MCBlock, BasicBlock> mcbb2bb = new HashMap<>();

    private Map<Function, MCFunction> funcMap = new HashMap<>();

    //private Map<Value, Reg> value2RegInFunc = new HashMap<>();

    private Label stack;
    private int stackSlot;

    public CodeGen(CompUnit compUnit) {
        this.compUnit = compUnit;
    }

    public MCUnit run() throws BackEndErr {

        //init stack
        //MCSpace space1 = new MCSpace(new Label("stack", null), AyaConfig.STACK_SIZE);
        //mcUnit.data.add(space1.getNode());
        //MCStack mcStack = new MCStack(AyaConfig.STACK_SIZE);
        //mcUnit.prelude.add(mcStack.getNode());
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
        mcFunction.label = new Label(f.getName(), null);
        mcUnit.list.add(mcFunction.getNode());
        value2Reg.clear();
        value2StackArr.clear();
        VReg.resetCnt();
        funcMap.put(f, mcFunction); // 小心递归函数

        //处理参数
        stackSlot = 0;
        //stackSlot += f.getParams().size(); // 按调用规范，放在寄存器中的参数也要分配栈空间
        stackSlot += 1; // for ra
        MCBlock b = getBlock();
        mcFunction.headBB = b;
        mcFunction.list.add(b.getNode()); //  加入函数入口块
        bb2mcbb.put(f.getFirstBB(), b);
        mcbb2bb.put(b, f.getFirstBB());

        MCStack push = new MCStack(0);
        b.list.add(push.getNode());
        mcFunction.push = push;
        Set<Reg> pdef = new HashSet<>(); //def物理寄存器
        List<Reg> paramPassed = new ArrayList<>(); //传参的寄存器
        List<Reg> paramVReg = new ArrayList<>();
        for (int i = 0; i < Math.min(4, f.getParams().size()); i++) {
            Param p = f.getParams().get(i);
            Reg r = VReg.alloc();
            MCInstr instr = new MCMove(r, PReg.getRegById(4 + i));
            b.list.add(instr.getNode()); // 添加参数寄存器到VReg的move
            // 特殊用途寄存器的liverange要尽量段，防止产生大量spill
            value2Reg.put(p, r);
            pdef.add(PReg.getRegById(4 + i));
            paramPassed.add(PReg.getRegById(4 + i));
            paramVReg.add(r);
        }
        if (!Objects.equals(mcFunction.label.name, "main")) {
            for (int i = f.getParams().size(); i < 4; i++) {
                Reg r = VReg.alloc();
                MCInstr instr = new MCMove(r, PReg.getRegById(4 + i));
                b.list.add(instr.getNode()); // 添加参数寄存器到VReg的move

                pdef.add(PReg.getRegById(4 + i));
                paramPassed.add(PReg.getRegById(4 + i));
                paramVReg.add(r);
            }
        }
        if (f.getParams().size() > 4) { // 超出寄存器范围的参数
            for (int i = 4; i < f.getParams().size(); i++) {
                Param p = f.getParams().get(i);
                Reg r = VReg.alloc();
                MCLw instr = new MCLw(PReg.getRegByName("sp"), r,  -i * 4-4);
                instr.isLoadArg = true;
                b.list.add(instr.getNode());
                value2Reg.put(p, r);
            }
        }

        List<Reg> calleeSaved = new ArrayList<>();
        List<PReg> tRegs = PReg.getTRegsWithoutAn();
        //tRegs.remove(PReg.getRegByName("v0"));
        //main 不需要保存被调用者保存寄存器
        //全部让callee保护
        if (!Objects.equals(mcFunction.label.name, "main")) {
            for (PReg pr: tRegs) {
                Reg r = VReg.alloc();
                MCMove instr = new MCMove(r, pr);
                b.list.add(instr.getNode());
                calleeSaved.add(r);
                pdef.add(pr);
            }
        }

        MCNop nop = new MCNop(pdef, new HashSet<>());
        b.list.addFirst(nop.getNode());

        for (MyNode<BasicBlock> bbNode :
                f.getList()) {
            BasicBlock bb = bbNode.getValue();
            if (bb == f.getFirstBB()) {
                genBlock(bb, b, calleeSaved, paramPassed, paramVReg);
            } else {
                MCBlock mcBlock = getBlock();
                bb2mcbb.put(bb, mcBlock);
                mcbb2bb.put(mcBlock,bb);
                genBlock(bb, mcBlock, calleeSaved, paramPassed, paramVReg);
                mcFunction.list.add(mcBlock.getNode());

            }
        }

        mcFunction.stackTop = stackSlot;
        stackSlot += f.getParams().size();
        mcFunction.stackSlot = stackSlot;
        //这时还没有生成处理栈的指令，因为部分虚拟寄存器应该分配到栈上，而实际上还没有寄存器分配
        //这时的stackSlot只包括局部数组ra和参数
        mappingReplace();
        destructPhi();
        mcFunction.regAllocated = VReg.getAllocateCounter();
        if (f.getName().equals("main")) {
            mcFunction.getNode().removeMe();
            mcUnit.list.addFirst(mcFunction.getNode());
        }

    }

    /**
     * 对函数里的占位符进行替换
     */
    public void mappingReplace() {
        for (MyNode<MCBlock> bbNode :
                mcFunction.list) {
            MCBlock mcbb = bbNode.getValue();
            BasicBlock bb = mcbb2bb.get(mcbb);
            mcbb.prec.addAll(bb.prec.stream().map(b-> bb2mcbb.get(b)).collect(Collectors.toList()));
            mcbb.succ.addAll(bb.succ.stream().map(b-> bb2mcbb.get(b)).collect(Collectors.toList()));
            for (MyNode<MCInstr> instrNode :
                    mcbb.list) {
                MCInstr mcInstr = instrNode.getValue();
                if (mcInstr instanceof MCInstrR) {
                    MCInstrR mcInstrR = (MCInstrR) mcInstr;
                    if (mcInstrR.s instanceof ValueReg) {
                        mcInstrR.s = value2Reg.get(((ValueReg) mcInstrR.s).value);
                    }
                    if (mcInstrR.t instanceof ValueReg) {
                        mcInstrR.t = value2Reg.get(((ValueReg) mcInstrR.t).value);
                    }
                }
                if (mcInstr instanceof MCInstrI) {
                    MCInstrI mcInstrI = (MCInstrI) mcInstr;
                    if (mcInstrI.s instanceof ValueReg) {
                        mcInstrI.s = value2Reg.get(((ValueReg) mcInstrI.s).value);
                    }
                }
                if (mcInstr instanceof MCInstrB) {
                    MCInstrB mcInstrB = (MCInstrB) mcInstr;
                    if (mcInstrB.s instanceof ValueReg) {
                        mcInstrB.s = value2Reg.get(((ValueReg) mcInstrB.s).value);
                    }
                    if (mcInstrB.t instanceof ValueReg) {
                        mcInstrB.t = value2Reg.get(((ValueReg) mcInstrB.t).value);
                    }
                    if (mcInstrB.target instanceof PsuMCBlock) {
                        mcInstrB.target = bb2mcbb.get(((PsuMCBlock) mcInstrB.target).basicBlock);
                    }
                }
                if (mcInstr instanceof MCJ) {
                    MCJ mcj = (MCJ) mcInstr;
                    if (mcj.target instanceof PsuMCBlock) {
                        mcj.target = bb2mcbb.get(((PsuMCBlock) mcj.target).basicBlock);
                    }
                }
                if (mcInstr instanceof MCJal) {
                    MCJal mcjal = (MCJal) mcInstr;
                    if (mcjal.target instanceof PsuMCBlock) {
                        mcjal.target = bb2mcbb.get(((PsuMCBlock) mcjal.target).basicBlock);
                    }
                }
                //jr only jr $ra
                //la 只会加载.data的地址
                if (mcInstr instanceof MCLw) {
                    MCLw mcLw = (MCLw) mcInstr;
                    if (mcLw.s instanceof ValueReg) {
                        mcLw.s = value2Reg.get(((ValueReg) mcLw.s).value);
                    }
                }
                if (mcInstr instanceof MCSw) {
                    MCSw mcSw = (MCSw) mcInstr;
                    if (mcSw.s instanceof ValueReg) {
                        mcSw.s = value2Reg.get(((ValueReg) mcSw.s).value);
                    }
                    if (mcSw.t instanceof ValueReg) {
                        mcSw.t = value2Reg.get(((ValueReg) mcSw.t).value);
                    }
                }
                if (mcInstr instanceof MCMove) {
                    MCMove mcMove = (MCMove) mcInstr;
                    if (mcMove.s instanceof ValueReg) {
                        mcMove.s = value2Reg.get(((ValueReg) mcMove.s).value);
                    }
                }
                if (mcInstr instanceof MCPhi) {
                    MCPhi mcPhi = (MCPhi) mcInstr;
                    List<Pair<Reg,MCBlock>> pairs = new ArrayList<>();
                    for (Pair<Reg, MCBlock> pair :
                            mcPhi.pairs) {
                        Pair<Reg,MCBlock> newPair = new Pair<>(
                                ((ValueReg)pair.getFirst()).value instanceof InitVal?
                                        pair.getFirst():
                                        value2Reg.get(((ValueReg)pair.getFirst()).value),
                                bb2mcbb.get(((PsuMCBlock)pair.getLast()).basicBlock)
                        );
                        pairs.add(newPair);
                    }
                    mcPhi.pairs = pairs;
                }
            }
        }
    }

    /**
     * 拆解phi，Briggs的论文实在看不懂，phi的def，use怎么算的也没说，就“有很多方法”，
     * 还是拆关键边吧
     * SSA Book P37
     */
    public void destructPhi() {
        //拆分关键边
        for (MCBlock mcbb:
             mcFunction.list.toList()) {
            Map<MCBlock, MCParallelCopy> bb2pc = new HashMap<>();
            for (MCBlock precBB :
                    new ArrayList<>(mcbb.prec)) {
                MCParallelCopy pc = new MCParallelCopy();
                if (mcbb.prec.size() > 1 && precBB.succ.size() > 1) {
                    MCBlock newBB = getBlock();
                    precBB.succ.remove(mcbb);
                    precBB.succ.add(newBB);
                    mcbb.prec.remove(precBB);
                    mcbb.prec.add(newBB);
                    replaceJumpTarget(precBB, mcbb, newBB);
                    replacePhiBlock(mcbb, precBB, newBB);
                    MCJ mcj = new MCJ(
                            mcbb
                    );
                    newBB.list.add(pc.getNode());
                    newBB.list.add(mcj.getNode());
                    newBB.prec.add(precBB);
                    newBB.succ.add(mcbb);
                    mcbb.getNode().insertAfter(newBB.getNode());
                    bb2pc.put(newBB,pc);
                } else {
                    MyNode<MCInstr> last = precBB.list.getLast();
                    while (last.getPrev() != null &&
                            (last.getPrev().getValue() instanceof MCJ ||
                                    last.getPrev().getValue() instanceof MCInstrB)) {
                        last = last.getPrev();
                    }
                    last.insertBefore(pc.getNode());
                    bb2pc.put(precBB,pc);
                }

            }
            for (MCInstr instr :
                    mcbb.list.toList()) {
                if (instr instanceof MCPhi) {
                    MCPhi phi = (MCPhi) instr;
                    for (Pair<Reg, MCBlock> p :
                            phi.pairs) {
                        bb2pc.get(p.getLast()).copies.add(
                                new Pair<>(phi.dest, p.getFirst())
                        );
                    }
                    phi.getNode().removeMe();
                }
            }
        }
        //并行复制串行化
        for (MyNode<MCBlock> mcbbNode:
                mcFunction.list) {
            MCBlock mcbb = mcbbNode.getValue();
            for (MCInstr mcInstr :
                    mcbb.list.toList()) {
                if (mcInstr instanceof MCParallelCopy) {
                    MCParallelCopy pcopy = (MCParallelCopy) mcInstr;
                    List<MCInstr> seq = new ArrayList<>();
                    while (!pcopy.copies.stream().allMatch(p-> p.getFirst() == p.getLast())) {
                        java.util.Optional<Pair<Reg, Reg>> s = pcopy.copies.stream().filter(
                                        p -> p.getFirst()!=p.getLast() &&
                                                pcopy.copies.stream().noneMatch(
                                                    p1->p1.getLast() == p.getFirst()))
                                .findAny();
                        if (s.isPresent()) {
                            Pair<Reg, Reg> pair = s.get();
                            if (pair.getLast() instanceof ValueReg) {
                                seq.add(new MCLi(
                                        ((InitVal)((ValueReg) pair.getLast()).value).getValue(),
                                        pair.getFirst())
                                );
                            } else {
                                seq.add(new MCMove(pair.getFirst(), pair.getLast()));
                            }

                            pcopy.copies.remove(pair);
                        } else {
                            Pair<Reg, Reg> pair = pcopy.copies.stream().filter(
                                    p -> p.getFirst() != p.getLast()
                            ).findAny().get();

                            VReg vReg = VReg.alloc();
                            if (pair.getLast() instanceof ValueReg) {
                                seq.add(new MCLi(
                                        ((InitVal)((ValueReg) pair.getLast()).value).getValue(),
                                        vReg)
                                );
                            } else {
                                seq.add(new MCMove(vReg, pair.getLast()));
                            }
                            pcopy.copies.remove(pair);
                            pcopy.copies.add(new Pair<>(pair.getFirst(), vReg));
                        }
                    }
                    for (MCInstr move :
                            seq) {
                        mcInstr.getNode().insertBefore(move.getNode());
                    }
                    mcInstr.getNode().removeMe();
                }
            }
        }
    }

    /**
     * 替换跳转目标，用于关键边拆分
     * @param old
     * @param nnew
     */
    private void replaceJumpTarget(MCBlock inWhich, MCBlock old,MCBlock nnew) {
        for (MyNode<MCInstr> instrNode :
                inWhich.list) {
            MCInstr instr = instrNode.getValue();
            if (instr instanceof MCJ) {
                MCJ mcj = (MCJ)instr;
                if (mcj.target == old) {
                    mcj.target = nnew;
                }
            }
            if (instr instanceof MCInstrB) {
                MCInstrB mcInstrB = (MCInstrB) instr;
                if (mcInstrB.target == old) {
                    mcInstrB.target = nnew;
                }
            }
        }
    }
    
    private void replacePhiBlock(MCBlock inWhich, MCBlock old, MCBlock nnew) {
        for (MyNode<MCInstr> instrNode :
                inWhich.list) {
            MCInstr instr = instrNode.getValue();
            if (instr instanceof MCPhi) {
                MCPhi phi = (MCPhi) instr;
                List<Pair<Reg,MCBlock>> pairs = new ArrayList<>();
                for (Pair<Reg, MCBlock> p :
                        phi.pairs) {
                    Pair<Reg,MCBlock> t;
                    if (p.getLast() == old) {
                        t = new Pair<>(p.getFirst(),nnew);
                    } else {
                        t = p;
                    }
                    pairs.add(t);
                }
                phi.pairs = pairs;
            }
        }
    }
    /**
     * 从SSA基本块向机器基本块生成代码
     *
     * @param bb   SSA基本块
     * @param mcbb 机器基本块
     */
    public void genBlock(BasicBlock bb, MCBlock mcbb, List<Reg> calleeSaved, List<Reg> paramPassed, List<Reg> paramVReg) throws BackEndErr {
        for (MyNode<Instr> instrNode :
                bb.getList()) {
            Instr instr = instrNode.getValue();
            if (instr instanceof BinaryOp) {
                translateBiOp(mcbb, (BinaryOp) instr);
            } else if (instr instanceof AllocInstr) {
                VReg vReg = VReg.alloc();
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
                MCInstr mcInstr = new MCInstrI(
                        vReg,
                        PReg.getRegByName("sp"),
                        (stackSlot - slot) * 4,
                        MCInstrI.Type.addu
                );// 保存数组地址
                mcbb.list.add(mcInstr.getNode());
                value2Reg.put(instr,vReg);
            } else if (instr instanceof ArrView) {
                VReg vReg = VReg.alloc();
                MCInstr mcInstr;
                if (((ArrView) instr).getArr() instanceof AllocInstr &&
                        ((AllocInstr) ((ArrView) instr).getArr()).getAllocType() == AllocInstr.AllocType.Static) {
                    //静态分配
                    VReg vReg1 = VReg.alloc();
                    Label label = value2Label.get(((ArrView) instr).getArr());
                    MCInstr mcInstr1 = new MCLa(
                            label,
                            vReg1
                    );
                    mcbb.list.add(mcInstr1.getNode());
                    if (((ArrView) instr).getIdx() instanceof InitVal) {
                        mcInstr = new MCInstrI(
                                vReg,
                                vReg1,
                                ((InitVal) ((ArrView) instr).getIdx()).getValue() * 4,
                                MCInstrI.Type.addu
                        );
                    } else {
                        VReg vReg2 = VReg.alloc();
                        MCInstr mcInstr2 = new MCInstrI(
                                vReg2,
                                new ValueReg(((ArrView) instr).getIdx()),
                                2,
                                MCInstrI.Type.sll
                        );
                        mcbb.list.add(mcInstr2.getNode());
                        mcInstr = new MCInstrR(
                                vReg,
                                vReg1,
                                vReg2,
                                MCInstrR.Type.addu
                        );
                    }
                } else {
                    if (((ArrView) instr).getIdx() instanceof InitVal) {
                        mcInstr = new MCInstrI(
                                vReg,
                                new ValueReg(((ArrView) instr).getArr()),
                                ((InitVal) ((ArrView) instr).getIdx()).getValue() * 4,
                                MCInstrI.Type.addu
                        );
                    } else {
                        VReg vReg2 = VReg.alloc();
                        MCInstr mcInstr2 = new MCInstrI(
                                vReg2,
                                new ValueReg(((ArrView) instr).getIdx()),
                                2,
                                MCInstrI.Type.sll
                        );
                        mcbb.list.add(mcInstr2.getNode());
                        mcInstr = new MCInstrR(
                                vReg,
                                new ValueReg(((ArrView) instr).getArr()),
                                vReg2,
                                MCInstrR.Type.addu
                        );

                    }
                }

                mcbb.list.add(mcInstr.getNode());
                value2Reg.put(instr,vReg);
            } else if (instr instanceof BrInstr) {
                if (((BrInstr) instr).getCond() instanceof InitVal) {
                    int cond = ((InitVal) ((BrInstr) instr).getCond()).getValue();
                    MCInstr j;
                    if (cond != 0) {
                        j = new MCJ(new PsuMCBlock(((BrInstr) instr).getBr0()));
                    } else {
                        j = new MCJ(new PsuMCBlock(((BrInstr) instr).getBr1()));
                    }
                    mcbb.list.add(j.getNode());
                } else {
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
                }

            } else if (instr instanceof BuiltinCallInstr) {
                if (((BuiltinCallInstr) instr).getFunc() == BuiltinCallInstr.Func.GetInt) {
                    VReg vReg = VReg.alloc();
                    MCInstr mcInstr = new MCLi(
                            5,
                            PReg.getRegByName("v0")
                    );
                    MCSyscall syscall = new MCSyscall();
                    MCInstr mcInstr1 = new MCMove(
                            vReg,
                            PReg.getRegByName("v0")
                    );
                    syscall.mayModify = true;
                    mcbb.list.add(mcInstr.getNode());
                    mcbb.list.add(syscall.getNode());
                    mcbb.list.add(mcInstr1.getNode());
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
            } else if (instr instanceof CallInstr) {
                MCFunction mcFunc = funcMap.get(((CallInstr) instr).getFunction());
                List<Value> params = ((CallInstr) instr).getParams();
                MCInstr mcInstr;
                //保存ra
                mcInstr = new MCSw(
                        PReg.getRegByName("sp"),
                        PReg.getRegByName("ra"),
                        0
                );
                mcbb.list.add(mcInstr.getNode());

                Set<Reg> jalUse = new HashSet<>();
                Set<Reg> jalDef = new HashSet<>();
                // 压入调用实参
                for (int i = 0; i < Math.min(params.size(), 4); i++) {
                    if (params.get(i) instanceof InitVal) {
                        mcInstr = new MCLi(
                                ((InitVal) params.get(i)).getValue(),
                                PReg.getRegById(4+i)
                        );
                    } else if (params.get(i) instanceof AllocInstr &&
                            ((AllocInstr) params.get(i)).getAllocType() == AllocInstr.AllocType.Static) {
                        mcInstr = new MCLa(
                                value2Label.get(params.get(i)),
                                PReg.getRegById(4+i)
                        );
                    } else {
                        mcInstr = new MCMove(
                                PReg.getRegById(4+i),
                                new ValueReg(params.get(i))
                        );
                    }

                    mcbb.list.add(mcInstr.getNode());
                    jalUse.add(PReg.getRegById(4+i));
                }
                for (int i = 4; i < params.size(); i++) {
                    if (params.get(i) instanceof InitVal) {
                        VReg vReg1 = VReg.alloc();
                        MCInstr mcInstr1 = new MCLi(((InitVal) params.get(i)).getValue(),vReg1);
                        mcbb.list.add(mcInstr1.getNode());
                        mcInstr = new MCSw(
                                PReg.getRegByName("sp"),
                                vReg1,
                                -4*i-4
                        );
                    } else if (params.get(i) instanceof AllocInstr &&
                            ((AllocInstr) params.get(i)).getAllocType() == AllocInstr.AllocType.Static) {
                        VReg vReg1 = VReg.alloc();
                        MCInstr mcInstr1 = new MCLa(
                                value2Label.get(params.get(i)),
                                PReg.getRegById(4+i)
                        );
                        mcbb.list.add(mcInstr1.getNode());
                        mcInstr = new MCSw(
                                PReg.getRegByName("sp"),
                                vReg1,
                                -4*i-4
                        );
                    } else {
                        mcInstr = new MCSw(
                                PReg.getRegByName("sp"),
                                new ValueReg(params.get(i)),
                                -4*i-4
                        );
                    }

                    mcbb.list.add(mcInstr.getNode());
                }
                MCInstr jal = new MCJal(mcFunc.headBB);
                mcbb.list.add(jal.getNode());
                VReg vReg = VReg.alloc();
                //获取返回值
                mcInstr = new MCMove(
                        vReg,
                        PReg.getRegByName("v0")
                );
                mcbb.list.add(mcInstr.getNode());
                jalDef.add(PReg.getRegByName("v0"));
                jal.getNode().insertAfter(new MCNop(jalDef,jalUse).getNode());
                mcInstr = new MCLw(
                        PReg.getRegByName("sp"),
                        PReg.getRegByName("ra"),
                        0
                );//加载ra
                mcbb.list.add(mcInstr.getNode());
                value2Reg.put(instr, vReg);

                //恢复ra
            } else if (instr instanceof JmpInstr) {
                MCInstr mcInstr = new MCJ(
                        new PsuMCBlock(
                                ((JmpInstr) instr).getTarget()
                        )
                );
                mcbb.list.add(mcInstr.getNode());
            } else if (instr instanceof LoadInstr) {
                Value arr = ((LoadInstr) instr).getPtr();
                MCInstr mcInstr;
                VReg vReg;

                if (arr instanceof AllocInstr) {
                    AllocInstr alloc = (AllocInstr) arr;
                    if (alloc.getAllocType() == AllocInstr.AllocType.Static) {
                        MCData data = mcUnit.getDataByName(alloc.getName());
                        vReg = VReg.alloc();
                        if (((LoadInstr) instr).getIndexes() instanceof InitVal) {
                            mcInstr = new MCLw(
                                  PReg.getRegById(0),
                                  vReg,
                                  data.label,
                                  ((InitVal) ((LoadInstr) instr).getIndexes()).getValue() * 4
                            );

                        } else {
                            VReg vReg1 = VReg.alloc();
                            MCInstr mcInstr1 = new MCInstrI(
                                    vReg1,
                                    new ValueReg(((LoadInstr) instr).getIndexes()),
                                    2,
                                    MCInstrI.Type.sll
                            );
                            mcbb.list.add(mcInstr1.getNode());
                            mcInstr = new MCLw(
                                    vReg1,
                                    vReg,
                                    data.label
                            );
                        }
                    } else {
                        //局部数组
                        vReg = VReg.alloc();
                        if (((LoadInstr) instr).getIndexes() instanceof InitVal) {
                            mcInstr = new MCLw(
                                    new ValueReg(((LoadInstr) instr).getPtr()),
                                    vReg,
                                    ((InitVal) ((LoadInstr) instr).getIndexes()).getValue() * 4
                            );

                        } else {
                            VReg vReg1 = VReg.alloc();
                            MCInstr mcInstr1 = new MCInstrI(
                                    vReg1,
                                    new ValueReg(((LoadInstr) instr).getIndexes()),
                                    2,
                                    MCInstrI.Type.sll
                            );
                            mcbb.list.add(mcInstr1.getNode());
                            MCInstr mcInstr2 = new MCInstrR(
                                    vReg1,
                                    vReg1,
                                    new ValueReg(((LoadInstr) instr).getPtr()),
                                    MCInstrR.Type.addu
                            );
                            mcbb.list.add(mcInstr2.getNode());

                            mcInstr = new MCLw(
                                    vReg1,
                                    vReg,
                                    0
                            );
                        }
                    }
                    mcbb.list.add(mcInstr.getNode());
                    value2Reg.put(instr,vReg);
                } else if (arr instanceof ArrView || arr instanceof Param) {
                    vReg = VReg.alloc();
                    if (((LoadInstr) instr).getIndexes() instanceof InitVal) {
                        mcInstr = new MCLw(
                                new ValueReg(((LoadInstr) instr).getPtr()),
                                vReg,
                                ((InitVal) ((LoadInstr) instr).getIndexes()).getValue() * 4
                        );

                    } else {
                        VReg vReg1 = VReg.alloc();
                        MCInstr mcInstr1 = new MCInstrI(
                                vReg1,
                                new ValueReg(((LoadInstr) instr).getIndexes()),
                                2,
                                MCInstrI.Type.sll
                        );
                        mcbb.list.add(mcInstr1.getNode());
                        MCInstr mcInstr2 = new MCInstrR(
                                vReg1,
                                vReg1,
                                new ValueReg(((LoadInstr) instr).getPtr()),
                                MCInstrR.Type.addu
                        );
                        mcbb.list.add(mcInstr2.getNode());

                        mcInstr = new MCLw(
                                vReg1,
                                vReg,
                                0
                        );
                    }
                    mcbb.list.add(mcInstr.getNode());
                    value2Reg.put(instr, vReg);
                }
            } else if (instr instanceof StoreInstr) {
                Value arr = ((StoreInstr) instr).getPtr();
                MCInstr mcInstr;
                Reg target;
                if (((StoreInstr) instr).getTarget() instanceof InitVal) {
                    VReg vReg1 = VReg.alloc();
                    MCInstr mcInstr1 = new MCLi(
                        ((InitVal) ((StoreInstr) instr).getTarget()).getValue(),
                            vReg1
                    );
                    target = vReg1;
                    mcbb.list.add(mcInstr1.getNode());
                } else {
                    target = new ValueReg(((StoreInstr) instr).getTarget());
                }
                if (arr instanceof AllocInstr) {
                    AllocInstr alloc = (AllocInstr) arr;
                    if (alloc.getAllocType() == AllocInstr.AllocType.Static) {
                        MCData data = mcUnit.getDataByName(alloc.getName());
                        if (((StoreInstr) instr).getIndex() instanceof InitVal) {
                            mcInstr = new MCSw(
                                    ((InitVal) ((StoreInstr) instr).getIndex()).getValue() * 4,
                                    PReg.getRegById(0),
                                    target,
                                    data.label
                            );

                        } else {
                            VReg vReg1 = VReg.alloc();
                            MCInstr mcInstr1 = new MCInstrI(
                                    vReg1,
                                    new ValueReg(((StoreInstr) instr).getIndex()),
                                    2,
                                    MCInstrI.Type.sll
                            );
                            mcbb.list.add(mcInstr1.getNode());
                            mcInstr = new MCSw(
                                    vReg1,
                                    target,
                                    data.label
                            );
                        }
                    } else {
                        if (((StoreInstr) instr).getIndex() instanceof InitVal) {
                            mcInstr = new MCSw(
                                    new ValueReg(((StoreInstr) instr).getPtr()),
                                    target,
                                    ((InitVal) ((StoreInstr) instr).getIndex()).getValue() * 4
                            );

                        } else {
                            VReg vReg1 = VReg.alloc();
                            MCInstr mcInstr1 = new MCInstrI(
                                    vReg1,
                                    new ValueReg(((StoreInstr) instr).getIndex()),
                                    2,
                                    MCInstrI.Type.sll
                            );
                            mcbb.list.add(mcInstr1.getNode());
                            MCInstr mcInstr2 = new MCInstrR(
                                    vReg1,
                                    vReg1,
                                    new ValueReg(((StoreInstr) instr).getPtr()),
                                    MCInstrR.Type.addu
                            );
                            mcbb.list.add(mcInstr2.getNode());

                            mcInstr = new MCSw(
                                    vReg1,
                                    target,
                                    0
                            );
                        }
                    }
                    mcbb.list.add(mcInstr.getNode());
                } else if (arr instanceof ArrView || arr instanceof Param) {
                    if (((StoreInstr) instr).getIndex() instanceof InitVal) {
                        mcInstr = new MCSw(
                                new ValueReg(((StoreInstr) instr).getPtr()),
                                target,
                                ((InitVal) ((StoreInstr) instr).getIndex()).getValue() * 4
                        );

                    } else {
                        VReg vReg1 = VReg.alloc();
                        MCInstr mcInstr1 = new MCInstrI(
                                vReg1,
                                new ValueReg(((StoreInstr) instr).getIndex()),
                                2,
                                MCInstrI.Type.sll
                        );
                        mcbb.list.add(mcInstr1.getNode());
                        MCInstr mcInstr2 = new MCInstrR(
                                vReg1,
                                vReg1,
                                new ValueReg(((StoreInstr) instr).getPtr()),
                                MCInstrR.Type.addu
                        );
                        mcbb.list.add(mcInstr2.getNode());

                        mcInstr = new MCSw(
                                vReg1,
                                target,
                                0
                        );
                    }
                    mcbb.list.add(mcInstr.getNode());
                }
            } else if (instr instanceof PhiInstr) {
                PhiInstr phiInstr = (PhiInstr) instr;
                VReg vReg = VReg.alloc();
                List<Pair<Reg, MCBlock>> list = new ArrayList<>();
                for (Pair<Value, BasicBlock> p : phiInstr.getPhiPairs()) {
                    Pair<Reg, MCBlock> regMCBlockPair;
                    /*if (p.getFirst() instanceof InitVal) {
                        VReg vReg1 = VReg.alloc();
                        MCLi mcLi = new MCLi(((InitVal) p.getFirst()).getValue(), vReg1);
                        mcbb.list.add(mcLi.getNode());
                        regMCBlockPair = new Pair<>(vReg1, new PsuMCBlock(p.getLast()));
                    } else {*/
                        regMCBlockPair = new Pair<>(new ValueReg(p.getFirst()), new PsuMCBlock(p.getLast()));
                    //}//这里的ValueReg里可能是InitVal
                    list.add(regMCBlockPair);
                }
                MCPhi phi = new MCPhi(
                        list,
                        null
                );
                phi.dest = vReg;
                mcbb.list.add(phi.getNode());
                value2Reg.put(instr, vReg);
            } else if (instr instanceof RetInstr) {
                if (mcFunction.label.name.equals("main")) {
                    mcFunction.pop = new MCStack(0);
                    MCInstr mcInstr = new MCJ(mcUnit.endProg);
                    mcbb.list.add(mcInstr.getNode());
                    continue;
                } //用MARS运行，不要给main生成返回语句

                Set<Reg> pused = new HashSet<>();
                List<PReg> tRegs = PReg.getTRegsWithoutAn();


                MCInstr mcInstr;
                if (((RetInstr) instr).getRetValue() != null) {
                    if (((RetInstr) instr).getRetValue() instanceof InitVal) {
                        mcInstr = new MCLi(
                                ((InitVal) ((RetInstr) instr).getRetValue()).getValue(),
                                PReg.getRegByName("v0")
                        );
                    } else {
                        mcInstr = new MCMove(
                                PReg.getRegByName("v0"),
                                new ValueReg(((RetInstr) instr).getRetValue())
                        );
                    }
                    mcbb.list.add(mcInstr.getNode());
                }
                MCStack pop = new MCStack(0);

                mcFunction.pop = pop;
                MCInstr mcInstr1 = new MCJr(
                        PReg.getRegByName("ra")
                );

                //tRegs.remove(PReg.getRegByName("v0"));
                if (calleeSaved.size() != tRegs.size()) {
                    throw new RuntimeException();
                }
                for (int i = 0; i < calleeSaved.size(); i++) {
                    Reg r = calleeSaved.get(i);

                    MCInstr mcInstr2 = new MCMove(
                            tRegs.get(i),
                            r
                    );
                    mcbb.list.add(mcInstr2.getNode());
                    pused.add(tRegs.get(i));
                } // 恢复被调用者保护寄存器
                for (int i = 0; i < paramPassed.size(); i++) {
                    Reg pReg = paramPassed.get(i);
                    if (paramPassed.contains(pReg)) {
                        MCInstr mcInstr2 = new MCMove(
                                pReg,
                                paramVReg.get(i)
                        );
                        mcbb.list.add(mcInstr2.getNode());
                        pused.add(pReg);
                    }
                }
                mcbb.list.add(pop.getNode());
                mcbb.list.add(mcInstr1.getNode());
                MCNop nop = new MCNop(new HashSet<>(), pused);
                mcbb.list.add(nop.getNode());
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
                Reg t = VReg.alloc();
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
                Reg t = VReg.alloc(); //at
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
                Reg t = VReg.alloc();
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
                    VReg vReg1 = VReg.alloc();
                    MCInstr mcInstr1 = new MCLi(
                            ((InitVal) left).getValue(),
                            vReg1
                    );
                    mcbb.list.add(mcInstr1.getNode());
                    mcInstr = new MCInstrR(
                            vReg, vReg1, new ValueReg(right), MCInstrR.Type.valueOf(s));
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
                MCInstr mcInstr = new MCInstrR(
                        vReg,
                        new ValueReg(right),
                        PReg.getRegById(0),
                        MCInstrR.Type.seq
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
                        VReg vReg1 = VReg.alloc();
                        mcInstr1 = new MCLi(
                                ((InitVal) right).getValue(),
                                vReg1
                        );
                        mcInstr = new MCInstrR(
                                vReg,
                                new ValueReg(left),
                                vReg1,
                                MCInstrR.Type.slt
                        );
                        mcbb.list.add(mcInstr1.getNode());
                        mcbb.list.add(mcInstr.getNode());
                    }
                } else if (left instanceof InitVal) {
                    VReg vReg1 = VReg.alloc();
                    mcInstr1 = new MCLi(
                            ((InitVal) left).getValue(),
                            vReg1
                    );
                    mcInstr = new MCInstrR(
                            vReg,
                            vReg1,
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

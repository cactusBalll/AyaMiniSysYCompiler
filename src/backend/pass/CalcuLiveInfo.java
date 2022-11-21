package backend.pass;

import backend.MCBlock;
import backend.MCFunction;
import backend.MCUnit;
import backend.instr.MCInstr;
import backend.regs.Reg;
import util.MyNode;

import java.util.*;

/**
 * 计算liveIn，liveOut信息
 */
public class CalcuLiveInfo implements MCPass{
    @Override
    public void run(MCUnit unit) {
        for (MyNode<MCFunction> funcNode :
                unit.list) {
            MCFunction func = funcNode.getValue();
            runOnFunction(func);
        }
    }

    private Map<MCBlock, BitSet> def = new HashMap<>();
    private Map<MCBlock, BitSet> use = new HashMap<>();
    private Map<MCBlock, BitSet> liveIn = new HashMap<>();
    private Map<MCBlock, BitSet> liveOut = new HashMap<>();
    /**
     * 位向量版
     * @param function 计算的函数
     */
    public void runOnFunctionOpt(MCFunction function) {
        def.clear();
        use.clear();
        liveIn.clear();
        liveOut.clear();
        // 预处理
        Map<Reg, Integer> reg2int = new HashMap<>();
        List<Reg> int2reg = new ArrayList<>();
        Set<Reg> usedRegs = new HashSet<>();
        function.list.forEach(bbNode-> bbNode.getValue().list.forEach(
                instrNode -> {
                    MCInstr instr = instrNode.getValue();
                    usedRegs.addAll(instr.getUse());
                    usedRegs.addAll(instr.getDef());
                }
        ));
        int i = 0;
        for (Reg r :
                usedRegs) {
            int2reg.add(r);
            reg2int.put(r, i);
            i++;
        }

        for (MyNode<MCBlock> bbNode :
                function.list) {
            MCBlock bb = bbNode.getValue();
            bb.def.clear();
            bb.use.clear();
            bb.liveIn.clear();
            bb.liveOut.clear();
            def.put(bb, new BitSet());
            use.put(bb, new BitSet());
            liveOut.put(bb, new BitSet());
            liveIn.put(bb, new BitSet());
            bb.list.forEach(
                    instrNode -> {
                        MCInstr instr = instrNode.getValue();
                        instr.getUse().forEach(reg -> {
                            if (!def.get(bb).get(reg2int.get(reg))) {
                                use.get(bb).set(reg2int.get(reg));
                            }
                        });
                        instr.getDef().forEach(reg -> {
                            if (!use.get(bb).get(reg2int.get(reg))) {
                                def.get(bb).set(reg2int.get(reg));
                            }
                        });
                    }
            );
        }
        changed = true;
        while (changed) {
            changed = false;
            visited.clear();
            walkMCBlockOpt(function.headBB);
        }

        //映射回去
        for (MyNode<MCBlock> bbNode :
                function.list) {
            MCBlock bb = bbNode.getValue();
            BitSet bitSet = liveIn.get(bb);
            int j = bitSet.nextSetBit(0);
            while (j != -1) {
                bb.liveIn.add(int2reg.get(j));
                j = bitSet.nextSetBit(j+1);
            }
            bitSet = liveOut.get(bb);
            j = bitSet.nextSetBit(0);
            while (j != -1) {
                bb.liveOut.add(int2reg.get(j));
                j = bitSet.nextSetBit(j+1);
            }
        }
    }

    public void walkMCBlockOpt(MCBlock nw) {
        if (visited.contains(nw)) {
            return;
        }
        visited.add(nw);
        nw.succ.forEach(this::walkMCBlockOpt);

        BitSet old = liveOut.get(nw);
        BitSet nwLiveOut = new BitSet();
        nw.succ.forEach(bb -> nwLiveOut.or(liveIn.get(bb)));
        if (!old.equals(nwLiveOut)) {
            changed = true;
        }
        liveOut.put(nw, nwLiveOut);

        old = liveIn.get(nw);

        BitSet nwLiveIn = new BitSet();

        nwLiveIn.or(use.get(nw));

        BitSet t = new BitSet();
        t.or(liveOut.get(nw));
        t.andNot(def.get(nw));

        nwLiveIn.or(t);

        if (!old.equals(nwLiveIn)) {
            changed = true;
        }
        liveIn.put(nw, nwLiveIn);
    }
    public void runOnFunction(MCFunction function) {
        // 设置每个基本块的def，use列表
        for (MyNode<MCBlock> bbNode :
                function.list) {
            MCBlock bb = bbNode.getValue();
            bb.def.clear();
            bb.use.clear();
            bb.liveIn.clear();
            bb.liveOut.clear();
            bb.list.forEach(
                    instrNode -> {
                        MCInstr instr = instrNode.getValue();
                        instr.getUse().forEach(reg -> {
                            if (!bb.def.contains(reg)) {
                                bb.use.add(reg);
                            }
                        });
                        instr.getDef().forEach(reg -> {
                            if (!bb.use.contains(reg)) {
                                bb.def.add(reg);
                            }
                        });
                    }
            );
        }
        changed = true;
        while (changed) {
            changed = false;
            visited.clear();
            walkMCBlock(function.headBB);
        }
    }
    private boolean changed;
    private Set<MCBlock> visited = new HashSet<>();
    private void walkMCBlock(MCBlock nw) {
        if (visited.contains(nw)) {
            return;
        }
        visited.add(nw);
        nw.succ.forEach(this::walkMCBlock);

        Set<Reg> old = nw.liveOut;
        nw.liveOut = new HashSet<>();
        nw.succ.forEach(bb -> nw.liveOut.addAll(bb.liveIn));
        if (!old.equals(nw.liveOut)) {
            changed = true;
        }

        old = nw.liveIn;
        nw.liveIn = new HashSet<>();
        nw.liveIn.addAll(nw.use);
        Set<Reg> t = new HashSet<>();
        // t = liveOut - Def
        for (Reg r :
                nw.liveOut) {
            if (!nw.def.contains(r)) {
                t.add(r);
            }
        }
        nw.liveIn.addAll(t);
        if (!old.equals(nw.liveIn)) {
            changed = true;
        }
    }
}

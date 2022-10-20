package ir.opt;

import ir.instruction.BrInstr;
import ir.instruction.Instr;
import ir.instruction.JmpInstr;
import ir.value.BasicBlock;
import ir.value.CompUnit;
import ir.value.Function;
import util.MyNode;

import java.util.ArrayList;
import java.util.BitSet;
import java.util.HashMap;

/**
 * 生成prec，succ，dom，idoming，idomer等信息
 */
public class BBInfo implements Pass{
    @Override
    public void run(CompUnit compUnit) {
        compUnit.forEveryFunction(function -> {
            calcuDominationForFunction(function);
            calculateDominanceDepth(function);
            calcuDominanceFrontierForFunction(function);
        });

    }
    // 从比赛照搬
    private static void calcuDominationForFunction(Function function) {
        ArrayList<BitSet> dom = new ArrayList<>();
        HashMap<BasicBlock, Integer> index = new HashMap<>();
        ArrayList<BasicBlock> rindex = new ArrayList<>();
        BitSet t = new BitSet();
        t.set(0);
        dom.add(t); // Dom(0) = {0}
        for (int i = 1; i < function.getList().getSize(); i++) {
            t = new BitSet();
            t.set(0,function.getList().getSize()-1);
            dom.add(t);
        } // forall n != 0, Dom(n) = N
        int cnt = 0;
        for (MyNode<BasicBlock> basicBlockMyNode : function.getList()) {
            BasicBlock bb = basicBlockMyNode.getValue();
            bb.clearBBInfo();
            index.put(bb,cnt);
            rindex.add(bb);
            cnt++;
        }
        boolean changed = true;
        while (changed) {
            changed = false;
            for (MyNode<BasicBlock> bbNode :
                    function.getList()) {
                BasicBlock bb = bbNode.getValue();
                BitSet temp = new BitSet();
                //temp.set(0,function.getList().getSize()-1);
                boolean first = true;
                for (BasicBlock predbb :
                        bb.prec) {
                    Integer i = index.get(predbb);
                    if (first) {
                        temp = (BitSet) dom.get(i).clone();
                        first = false;
                    } else {
                        temp.and(dom.get(i));
                    }
                } //前驱dom的交
                Integer ndomIndex = index.get(bb);
                temp.set(ndomIndex); //并自己
                if (!temp.equals(dom.get(ndomIndex))) {
                    dom.set(ndomIndex, temp);
                    changed = true;
                }
            }
        }

        // 把bitset的信息更新到BasicBlock
        for (int i = 0, rindexSize = rindex.size(); i < rindexSize; i++) {
            BasicBlock bb = rindex.get(i);
            int domer = dom.get(i).nextSetBit(0);
            while (domer != -1) {
                bb.domer.add(rindex.get(domer));
                domer = dom.get(i).nextSetBit(domer + 1);
            }
        }
        function.getList().forEach(basicBlockMyNode -> {
            BasicBlock bb = basicBlockMyNode.getValue();
            bb.domer.forEach(dombb -> dombb.doming.add(bb));
        }); // 计算被支配(dominating)信息

        // 计算immediate dominator
        for (int i = 1, domSize = dom.size(); i < domSize; i++) {
            BitSet domI = dom.get(i);
            int possibleIdom = domI.nextSetBit(0);
            while (possibleIdom != -1) {
                boolean isIdom = true;
                int otherNode = domI.nextSetBit(0);
                if (possibleIdom == i) { // idom 不是自己
                    possibleIdom = domI.nextSetBit(possibleIdom + 1);
                    continue;
                }
                while (otherNode != -1) { //判断是不是其他必经节点的必经节点
                    if (otherNode != possibleIdom && otherNode != i && dom.get(otherNode).get(possibleIdom)) {
                        isIdom = false;
                        break;
                    }
                    otherNode = domI.nextSetBit(otherNode + 1);
                }
                if (isIdom) {
                    rindex.get(i).idomer = rindex.get(possibleIdom);
                    rindex.get(possibleIdom).idoming.add(rindex.get(i)); // 双向建边，构成支配树
                    break;
                }
                possibleIdom = domI.nextSetBit(possibleIdom + 1);
            }
        }

    }
    private static void calculateDominanceDepth(Function function) {
        dfsDomTree(0,function.getList().getFirst().getValue());
    }

    private static void dfsDomTree(int depth, BasicBlock nw) {
        nw.domDepth = depth;
        for (BasicBlock bb :
                nw.idoming) {
            dfsDomTree(depth+1,bb);
        }
    }

    private static void calcuDominanceFrontierForFunction(Function function) {
        for (MyNode<BasicBlock> bbNode :
                function.getList()) {
            BasicBlock bb = bbNode.getValue();

            for (BasicBlock succ :
                    bb.succ) {
                //bb 可能为null因为入口节点没有直接支配节点。
                BasicBlock x = bb;
                BasicBlock b =succ;
                while (x != null && (!x.doming.contains(b) || x == b)) {
                    x.df.add(b);
                    x = x.idomer;
                }
            }
        }
    }
}

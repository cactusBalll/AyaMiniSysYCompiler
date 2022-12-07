package ir.opt;

import ir.instruction.*;
import ir.value.*;
import util.MyNode;
import util.Pair;

import java.util.*;

public class GVNGCM implements Pass{
    private Map<BinaryOp.Wrapper, BinaryOp> computed = new HashMap<>();
    private Map<ArrView.Wrapper, ArrView> arrViewComputed = new HashMap<>();

    private Map<CallInstr.Wrapper, CallInstr> pureCallComputed = new HashMap<>();

    private List<LoadInstr> localLoads = new ArrayList<>(); //局部加载指令可能可以公共子表达式删除
    private Set<BasicBlock> visited = new HashSet<>();

    private Set<Instr> instrVisited = new HashSet<>();

    private Map<Instr, BasicBlock> block = new HashMap<>();

    private Function function = null;

    @Override
    public void run(CompUnit compUnit) {
        compUnit.forEveryFunction(function1 -> {
            GVNOnFunction(function1);
            compUnit.fullMaintain();
            GCMOnFunction(function1);
        });
    }

    public void GVNOnFunction(Function function) {
        //GVN
        computed.clear();
        arrViewComputed.clear();
        pureCallComputed.clear();
        visited.clear();
        RPOwalkFunction(function.getFirstBB());
    }
    /**
     * 逆后序遍历，大概是图上深搜？（因为CFG是图，不能是前序）
     * @param basicBlock 当前基本块
     */
    private void RPOwalkFunction(BasicBlock basicBlock) {
        localLoads.clear();
        visited.add(basicBlock);
        List<Instr> toRemove = new ArrayList<>();
        for (MyNode<Instr> instrNode:
            basicBlock.getList()) {
            Instr instr = instrNode.getValue();
            if (instr instanceof BinaryOp) {
                if (computed.containsKey(((BinaryOp) instr).getWrapper())) {
                    BinaryOp subs = computed.get(((BinaryOp) instr).getWrapper());
                    instr.replaceAllUsesOfMeWith(subs);
                    instr.removeMeFromAllMyUses();
                    toRemove.add(instr);
                } else {
                    computed.put(((BinaryOp) instr).getWrapper(), (BinaryOp) instr);
                }
            }
            if (instr instanceof ArrView) {
                if (arrViewComputed.containsKey(((ArrView) instr).getWrapper())) {
                    ArrView subs = arrViewComputed.get(((ArrView) instr).getWrapper());
                    instr.replaceAllUsesOfMeWith(subs);
                    instr.removeMeFromAllMyUses();
                    toRemove.add(instr);
                } else {
                    arrViewComputed.put(((ArrView) instr).getWrapper(), (ArrView) instr);
                }
            }
            if (instr instanceof CallInstr) {
                CallInstr callInstr = (CallInstr) instr;
                localLoads.clear();
                if (callInstr.getFunction().isPure()) {
                    if (pureCallComputed.containsKey(callInstr.getWrapper())) {
                        CallInstr subs = pureCallComputed.get(callInstr.getWrapper());
                        instr.replaceAllUsesOfMeWith(subs);
                        instr.removeMeFromAllMyUses();
                        toRemove.add(instr);
                    }else {
                        pureCallComputed.put(callInstr.getWrapper(), callInstr);
                    }
                }
            }
            if (instr instanceof StoreInstr) {
                StoreInstr storeInstr = (StoreInstr) instr;
                if (storeInstr.getPtr() instanceof Param) { // 对参数存，可能改变任意Mem位置
                    localLoads.clear();
                }
                if (storeInstr.getPtr() instanceof AllocInstr) {
                    localLoads.removeIf(loadInstr -> loadInstr.getPtr() == storeInstr.getPtr());
                }
                if (storeInstr.getPtr() instanceof ArrView) {
                    Value v = resolveArrView((ArrView) storeInstr.getPtr());
                    if (v instanceof AllocInstr) {
                        localLoads.removeIf(loadInstr -> loadInstr.getPtr() == v);
                    }
                    if (v instanceof Param) {
                        localLoads.clear();
                    }
                }
            }
            if (instr instanceof LoadInstr) {
                LoadInstr loadInstr = null;
                for (LoadInstr l :
                        localLoads) {
                    if (l.getPtr() == ((LoadInstr) instr).getPtr() &&
                            l.getIndexes() == ((LoadInstr) instr).getIndexes()) {
                        loadInstr = l;
                        break;
                    }
                }
                if (loadInstr != null) {
                    instr.replaceAllUsesOfMeWith(loadInstr);
                    instr.removeMeFromAllMyUses();
                    toRemove.add(instr);
                } else {
                    localLoads.add((LoadInstr) instr);
                }
            }
        }
        toRemove.forEach(instr -> instr.getNode().removeMe());
        for (int i = basicBlock.succ.size()-1; i >= 0; i--) {
            BasicBlock basicBlock1 = basicBlock.succ.get(i);
            if (!visited.contains(basicBlock1)) {
                RPOwalkFunction(basicBlock1);
            }
        }
    }

    private Value resolveArrView(ArrView arrView) {
        Value t = arrView;
        while (t instanceof ArrView) {
            t = ((ArrView) t).getArr();
        }
        return t;
    }
    private void GCMOnFunction(Function function) {
        instrVisited.clear();
        block.clear();
        this.function = function;
        for (MyNode<BasicBlock> bbNode :
                function.getList()) {
            BasicBlock bb = bbNode.getValue();
            for (MyNode<Instr> instrNode :
                    bb.getList()) {
                Instr instr = instrNode.getValue();
                block.put(instr,bb);
            }
        }

        this.function = function;

        //schedule early
        for (MyNode<BasicBlock> bbNode :
                function.getList()) {
            BasicBlock bb = bbNode.getValue();
            for (MyNode<Instr> instrNode :
                    bb.getList()) {
                Instr instr = instrNode.getValue();
                if (isPinned(instr)) {
                    instrVisited.add(instr);
                    for (MyNode<Value> valueNode :
                            instr.getUses()) {
                        Value value = valueNode.getValue();
                        if (value instanceof Instr) {
                            scheduleEarly((Instr) value);
                        }
                    }
                }
            }
        }
        // 不被pinned指令直接或间接使用的指令理论上是死代码，
        // 但是不对所有指令schedule会bug
        for (MyNode<BasicBlock> bbNode :
                function.getList()) {
            BasicBlock bb = bbNode.getValue();
            for (MyNode<Instr> instrNode :
                    bb.getList()) {
                Instr instr = instrNode.getValue();
                if (!instrVisited.contains(instr)) {
                    scheduleEarly(instr);
                }
            }
        }

        //schedule late
        instrVisited.clear();
        for (MyNode<BasicBlock> bbNode :
                function.getList()) {
            BasicBlock bb = bbNode.getValue();
            for (Instr instr :
                    bb.getList().toList()) {
                //var instr = instrNode.getValue();
                if (isPinned(instr)) {
                    instrVisited.add(instr);
                    for (MyNode<User> UserNode :
                            instr.getUsers()) {
                        User user = UserNode.getValue();
                        if (user instanceof Instr) {
                            scheduleLate((Instr) user);
                        }
                    }
                }
            }
        }
        // 不直接或者间接使用pinned instruction的指令理论上都可以编译期计算。
        // 同理，也是为了保证所有指令都有移动的机会，防止GVN产生问题
        for (MyNode<BasicBlock> bbNode :
                function.getList()) {
            BasicBlock bb = bbNode.getValue();
            for (Instr instr :
                    bb.getList().toList()) {
                if (!instrVisited.contains(instr)) {
                    scheduleLate(instr);
                }
            }
        }

    }

    private void scheduleEarly(Instr instr) {
        if (instrVisited.contains(instr) || isPinned(instr)) {
            return;
        }
        instrVisited.add(instr);
        block.put(instr, function.getList().getFirst().getValue());
        List<BasicBlock> uses = new ArrayList<>();
        for (MyNode<Value> valueNode :
                instr.getUses()) {
            Value value = valueNode.getValue();
            if (value instanceof Instr) {
                Instr input = (Instr)value;
                scheduleEarly(input);
                if (block.get(input).domDepth >
                        block.get(instr).domDepth) {
                    block.put(instr,block.get(input));
                }
                uses.add(block.get(input));
            }
        }
        //check
        for (BasicBlock input :
                uses) {
            assert input.doming.contains(block.get(instr));
        }
    }

    private void scheduleLate(Instr instr) {
        if (instrVisited.contains(instr) ||isPinned(instr)) {
            return;
        }
        instrVisited.add(instr);
        BasicBlock lca = null;
        List<BasicBlock> usebb = new ArrayList<>();
        for (MyNode<User> userNode :
                instr.getUsers()) {
            User user = userNode.getValue();
            if (user instanceof Instr) {
                Instr userInstr = (Instr)user;
                scheduleLate(userInstr);
                BasicBlock use = block.get(userInstr);
                if (userInstr instanceof PhiInstr) {
                    PhiInstr phi = (PhiInstr)userInstr;
                    for (Pair<Value, BasicBlock> entry :
                            phi.getPhiPairs()) {
                        if (entry.getFirst() == instr) {
                            use = entry.getLast();
                            usebb.add(use);
                            // 有可能这个phi分支两个的来源都是一样的
                            lca = find_lca(lca, use);
                        }
                    }
                } else {
                    usebb.add(use);
                    lca = find_lca(lca, use);
                }
            }
        }
        if (lca == null) {
            // 没有对这条指令的使用
            // 应该在CCP删掉
            lca = block.get(instr);
        }

        //check lca dominate 所有使用
        for (BasicBlock use :
                usebb) {
            assert lca.doming.contains(use);
        }

        BasicBlock best = lca;
        assert lca.domDepth >= block.get(instr).domDepth;
        while (true) {
            if (lca.loopDepth < best.loopDepth) {
                best = lca;
            }
            if (lca.idomer == null) {
                break;
            }
            if (lca == block.get(instr)) {
                break;
            }
            lca = lca.idomer;
        }
        // check best dominate 所有使用
        for (BasicBlock use :
                usebb) {
            assert best.doming.contains(use);
        }

        block.put(instr, best);
        scheduleInstr(instr,best);
    }
    private void scheduleInstr(Instr instr, BasicBlock basicBlock) {
        instr.getNode().removeMe();
        instr.bbBelongTo = basicBlock;
        // 虽然算法保证在这个bb是合法的，但是具体在那个位置还需计算
        List<Instr> list = basicBlock.getList().toList();
        int late = list.size() - 1;
        for (MyNode<User> userNode :
                instr.getUsers()) {
            User user = userNode.getValue();
            if (user instanceof Instr) {
                Instr userInstr = (Instr)user;
                int t = list.indexOf(userInstr);
                if (t != -1) {
                    late = Math.min(late, t);
                }
            }
        }
        if (late >= 1 && list.get(late-1) instanceof BinaryOp) {
            BinaryOp bi = (BinaryOp)list.get(late-1);

        }
        list.get(late).getNode().insertBefore(instr.getNode());
    }
    private BasicBlock find_lca(BasicBlock a, BasicBlock b) {
        // 最近公共祖先
        if (a == null) {
            return b;
        }
        while (a.domDepth > b.domDepth) {
            if (a.idomer == null) {
                return a; // 入口节点没有直接支配者
            }
            a = a.idomer;
        }
        while (b.domDepth > a.domDepth) {
            if (b.idomer== null) {
                return b;
            }
            b = b.idomer;
        }
        while (a != b) {
            a = a.idomer;
            b = b.idomer;
        }
        return a;
    }
    private boolean isPinned(Instr instr) {
        return  instr instanceof PhiInstr || instr instanceof BrInstr ||
                instr instanceof JmpInstr ||
                (instr instanceof CallInstr && !((CallInstr) instr).getFunction().isPure()) ||
                instr instanceof BuiltinCallInstr || instr instanceof StoreInstr ||
                instr instanceof LoadInstr || instr instanceof RetInstr ||
                (instr instanceof BinaryOp && ((BinaryOp) instr).isCondSet());
    }
}

package ir.opt;

import ir.instruction.*;
import ir.value.*;
import ty.IntTy;
import ty.Ty;
import util.MyNode;
import util.Pair;

import java.util.*;

public class Mem2Reg implements Pass{
    private Map<Value, Deque<Value>> reachingDef;
    private Set<BasicBlock> visited;

    private Map<Instr, Value> owner;
    @Override
    public void run(CompUnit compUnit) {
        compUnit.forEveryFunction(this::mem2RegOnFunction);
    }

    private void mem2RegOnFunction(Function function) {
        reachingDef = new HashMap<>();
        visited = new HashSet<>();
        owner = new HashMap<>();
        BasicBlock entryBB =
                function.getList().getFirst().getValue();

        for (MyNode<Instr> instrNode :
                entryBB.getList()) {
            Instr instr = instrNode.getValue();
            if (instr instanceof AllocInstr) {
                AllocInstr allocInstr = (AllocInstr) instr;
                if (allocInstr.getAllocTy() instanceof IntTy) {
                    insertPhi(allocInstr);
                }
            }
        }

        for (Param p :
                function.getParams()) {
            if (p.getType() instanceof IntTy) {
                insertPhi(p);
                reachingDef.get(p).push(p);
                p.bbBelongTo = entryBB;
            }
        }
        renamingVisitBlock(entryBB);
        // remove nop
        for (MyNode<BasicBlock> bbNode :
                function.getList()) {
            BasicBlock bb = bbNode.getValue();
            ArrayList<MyNode<Instr>> toRemove = new ArrayList<>();
            for (MyNode<Instr> instrNode :
                    bb.getList()) {
                Instr instr = instrNode.getValue();
                if (instr.isNop) {
                    toRemove.add(instrNode);
                }
            }
            for (MyNode<Instr> instrNode: toRemove) {
                instrNode.getValue().removeMeFromAllMyUses();
                instrNode.removeMe();
            }
        }
    }

    private void insertPhi(Value allocInstr) {
        reachingDef.put(allocInstr, new ArrayDeque<>());
        Set<BasicBlock> W = new HashSet<>();
        Set<BasicBlock> F = new HashSet<>();
        for (MyNode<User> userNode :
                allocInstr.getUsers()) {
            User user = userNode.getValue();
            if (user instanceof StoreInstr) {
                StoreInstr storeInstr = (StoreInstr) user;
                W.add(storeInstr.bbBelongTo);
            }
        }
        while (!W.isEmpty()) {
            BasicBlock bb = W.iterator().next();
            W.remove(bb);
            for (BasicBlock domfrontBB :
                    bb.df) {
                if (!F.contains(domfrontBB)) {
                    PhiInstr phiInstr = new PhiInstr(new ArrayList<>());
                    phiInstr.bbBelongTo = domfrontBB;
                    owner.put(phiInstr, allocInstr);
                    domfrontBB.getList().addFirst(phiInstr.getNode());
                    F.add(domfrontBB);
                    boolean hasDefV = false;
                    for (MyNode<Instr> domBBInstrNode :
                            domfrontBB.getList()) {
                        Instr domBBInstr = domBBInstrNode.getValue();
                        if (domBBInstr instanceof StoreInstr &&
                            ((StoreInstr) domBBInstr).getPtr() == allocInstr) {
                            hasDefV = true;
                        }
                    }  // 是否对这个alloca的变量进行了定值
                    if (!hasDefV) {
                        W.add(domfrontBB);
                    }
                }
            }
        }
    }
    private void renamingVisitBlock(BasicBlock bb) {
        if (visited.contains(bb)) {
            return;
        }
        visited.add(bb);
        for (MyNode<Instr> instrNode :
                bb.getList()) {
            Instr instr = instrNode.getValue();
            if (instr instanceof AllocInstr) { // 可能有二重指针，必须做完整的判断
                if (((AllocInstr) instr).getAllocTy() instanceof IntTy) {
                    instr.isNop = true;
                }
            }
            if (instr instanceof LoadInstr) {
                LoadInstr loadInstr = (LoadInstr) instr;
                Value ptr = loadInstr.getPtr();
                Ty ty;
                if (ptr instanceof AllocInstr) {
                    ty = ((AllocInstr) ptr).getAllocTy();
                } else {
                    ty = ptr.getType();
                }

                if (ty instanceof IntTy && !isStaticAlloc(ptr)) {
                    // 全局变量不需要转换
                    updateReachingDef(ptr, bb);

                    Value dominatingInstr = reachingDef.get(ptr).peek();
                    Value val = null;
                    if (dominatingInstr instanceof StoreInstr) {
                        val = ((StoreInstr) dominatingInstr).getTarget();
                    }else if (dominatingInstr instanceof PhiInstr) {
                        val = dominatingInstr;
                    } else if (dominatingInstr instanceof Param) {
                        val = dominatingInstr;
                    } else {
                        assert false;
                    }
                    instr.replaceAllUsesOfMeWith(val);
                    instr.isNop = true;
                }
            }
            if (instr instanceof StoreInstr) {
                StoreInstr storeInstr = (StoreInstr) instr;
                Value ptr = storeInstr.getPtr();
                Ty ty;
                if (ptr instanceof AllocInstr) {
                    ty = ((AllocInstr) ptr).getAllocTy();
                } else {
                    ty = ptr.getType();
                }

                if (ty instanceof IntTy &&
                        !isStaticAlloc(ptr)) {
                    updateReachingDef(ptr, bb);
                    reachingDef.get(ptr).push(storeInstr);
                    instr.isNop = true;
                }
            }

            if (instr instanceof PhiInstr) {
                PhiInstr phiInstr = (PhiInstr) instr;
                if (!reachingDef.containsKey(owner.get(phiInstr))) {
                    // 不是这次mem2reg加入的phi
                    continue;
                }
                updateReachingDef(owner.get(phiInstr), bb);
                reachingDef.get(owner.get(phiInstr)).push(phiInstr);
            }
        }
        // 维护后继节点的phi
        for (BasicBlock succbb:
                bb.succ) {
            for (MyNode<Instr> instrNode :
                    succbb.getList()) {

                Instr instr = instrNode.getValue();
                if (instr instanceof PhiInstr) {
                    PhiInstr phiInstr = (PhiInstr) instr;
                    if (!reachingDef.containsKey(owner.get(phiInstr))) {
                        // 不是这次mem2reg加入的phi
                        continue;
                    }
                    updateReachingDef(owner.get(phiInstr), bb);
                    Value dominatingInstr = reachingDef.get(owner.get(phiInstr)).peek();
                    Value val = null;
                    if (dominatingInstr instanceof StoreInstr) {
                        val = ((StoreInstr) dominatingInstr).getTarget();
                    }else if (dominatingInstr instanceof PhiInstr) {
                        val = dominatingInstr;
                    } else if (dominatingInstr instanceof Param) {
                        val = dominatingInstr;
                    } else {
                        assert false;
                    }
                    phiInstr.putItem(new Pair<>(val,bb));
                    bb.getUsers().add((MyNode<User>) phiInstr.getNode());
                    val.getUsers().add((MyNode<User>) phiInstr.getNode());
                }
            }
        }
        for (BasicBlock dombb:
                bb.idoming) {
            renamingVisitBlock(dombb);
        }
    }

    private static boolean isStaticAlloc(Value ptr) {
        return (ptr instanceof AllocInstr &&
                ((AllocInstr) ptr).getAllocType() == AllocInstr.AllocType.Static);
    }

    private void updateReachingDef(Value v, BasicBlock bb) {
        while (true) {
            Value def = reachingDef.get(v).peek();
            //assert def != null; //变量必须先定义再使用
            if (def == null) {
                break;
            }
            if (def.bbBelongTo.doming.contains(bb)) {
                break;
            }
            reachingDef.get(v).pop();
        }
    }
}

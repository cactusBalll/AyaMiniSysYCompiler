package ir.opt;

import Driver.AyaConfig;
import ir.IRCloner;
import ir.instruction.*;
import ir.value.BasicBlock;
import ir.value.CompUnit;
import ir.value.Function;
import ir.value.User;
import util.MyList;
import util.MyNode;

import java.util.ArrayList;
import java.util.List;

public class FuncInline implements Pass{
    @Override
    public void run(CompUnit compUnit) {
        List<MyNode<Function>> inlinedFunctions = new ArrayList<>();
        compUnit.forEveryFunction(function -> {
            if (canInline(function)) {
                List<CallInstr> subsCalls = new ArrayList<>();
                for (MyNode<User> user: function.getUsers()) {
                    CallInstr callInstr = (CallInstr) user.getValue();
                    Function inlineFunc = IRCloner.getInstance().cloneFunction(function);
                    inlineFunc.forEveryBasicBlock(basicBlock -> basicBlock.loopDepth += callInstr.bbBelongTo.loopDepth);
                    int paramLen = callInstr.getParams().size();
                    for (int i = 0; i < paramLen; i++) {
                        // 把对函数形参的使用替换成对函数调用时传入实参的使用
                        inlineFunc.getParams().get(i)
                                .replaceAllUsesOfMeWith(callInstr.getParams().get(i));
                    }

                    if (function.getList().getSize() == 1) {
                        // 只有一个基本块
                        // 处理返回值
                        // 貌似只有一个ret
                        RetInstr ret = findRet(inlineFunc);

                        // 把对函数调用的使用替换成对返回值的使用
                        if (!ret.isRetNull()) {
                            callInstr.replaceAllUsesOfMeWith(ret.getRetValue());
                        }

                        ret.removeMeFromAllMyUses();
                        ret.getNode().removeMe();

                        for (Instr instr :
                                inlineFunc.getList().getFirst().getValue().getList().toList()) {
                            // 把被内联函数的指令插入目标函数
                            // 注意维护一些关系
                            //var instr = instrNode.getValue();
                            if (!(instr instanceof RetInstr)) {
                                instr.getNode().setOwner(callInstr.getNode().getOwner());
                                instr.bbBelongTo = callInstr.bbBelongTo;
                                callInstr.getNode().insertBefore(instr.getNode());
                            }
                        }
                        //callInstr.getNode().removeMe();
                        subsCalls.add(callInstr);
                    } else {
                        // 存在多于一个的基本块（至少三个?）
                        // 处理返回值
                        // 貌似只有一个ret
                        RetInstr ret = findRet(inlineFunc);

                        // 把对函数调用的使用替换成对返回值的使用
                        if (!ret.isRetNull()) {
                            callInstr.replaceAllUsesOfMeWith(ret.getRetValue());
                        }

                        BasicBlock retBlock = ret.bbBelongTo;
                        ret.getNode().removeMe();
                        // 以callInstr为界，把bb分成两块，方便把内联的函数缝进去
                        BasicBlock splitBlock = new BasicBlock(callInstr.bbBelongTo);


                        MyList<Instr> splitBlockInstrList = callInstr.bbBelongTo.getList().removeAllAfter(callInstr.getNode());;
                        //这里可能设置错误

                        int size = callInstr.bbBelongTo.getList().getSize();
                        for (MyNode<Instr> InstrNode :
                                splitBlockInstrList) {
                            Instr instr = InstrNode.getValue();
                            instr.bbBelongTo = splitBlock;
                        }
                        splitBlock.setList(splitBlockInstrList);
                        callInstr.bbBelongTo.getNode().insertAfter(splitBlock.getNode());

                        Instr brInstrOfCallBlock = splitBlock.getList().getLast().getValue();
                        if (brInstrOfCallBlock instanceof BrInstr) {
                            BrInstr br = (BrInstr)brInstrOfCallBlock;
                            if (br.getBr0() != null) {
                                for (MyNode<Instr> instrNode :
                                        br.getBr0().getList()) {
                                    Instr instr = instrNode.getValue();
                                    if (instr instanceof PhiInstr) { // 只有Phi该换，下面同理
                                        instr.replaceUseWith(callInstr.bbBelongTo, splitBlock);
                                    }
                                }
                            }
                            if (br.getBr1() != null) {
                                for (MyNode<Instr> instrNode :
                                        br.getBr1().getList()) {
                                    Instr instr = instrNode.getValue();
                                    if (instr instanceof PhiInstr) {
                                        instr.replaceUseWith(callInstr.bbBelongTo, splitBlock);
                                    }
                                }
                            }
                        }
                        if (brInstrOfCallBlock instanceof JmpInstr) {
                            JmpInstr jmpInstr = (JmpInstr) brInstrOfCallBlock;
                            for (MyNode<Instr> instrNode :
                                    jmpInstr.getTarget().getList()) {
                                Instr instr = instrNode.getValue();
                                if (instr instanceof PhiInstr) { // 只有Phi该换，下面同理
                                    instr.replaceUseWith(callInstr.bbBelongTo, splitBlock);
                                }
                            }
                        }

                        for (Instr instr:
                                inlineFunc.getList().getFirst().getValue().getList().toList()) {
                            // 把被内联函数的指令插入目标函数
                            // 注意维护一些关系
                            instr.getNode().setOwner(callInstr.getNode().getOwner());
                            instr.bbBelongTo = callInstr.bbBelongTo;
                            // 把跳转到被内联函数出口块的指令重定向到splitBlock
                            instr.replaceUseWith(retBlock, splitBlock);
                            // 把对被内联函数入口块的使用替换成对callInstr所在block的使用
                            instr.replaceUseWith(inlineFunc.getList().getFirst().getValue(), callInstr.bbBelongTo);
                            callInstr.getNode().insertBefore(instr.getNode());
                        }


                        // 把返回块缝到splitBlock
                        MyNode<Instr> firstInstr = splitBlock.getList().getFirst();
                        for (Instr instr:
                                retBlock.getList().toList()) {
                            // 把被内联函数的指令插入目标函数
                            // 注意维护一些关系
                            instr.getNode().setOwner(splitBlock.getList());
                            instr.bbBelongTo = splitBlock;
                            // 把跳转到被内联函数出口块的指令重定向到splitBlock
                            instr.replaceUseWith(retBlock, splitBlock);
                            // 把对被内联函数入口块的使用替换成对callInstr所在block的使用
                            instr.replaceUseWith(inlineFunc.getList().getFirst().getValue(), callInstr.bbBelongTo);
                            if (firstInstr != null) {
                                firstInstr.insertBefore(instr.getNode());
                            } else {
                                splitBlock.getList().addLast(instr.getNode());
                            }
                        }

                        List<BasicBlock> t = new ArrayList<>();
                        for (MyNode<BasicBlock> bbNode :
                                inlineFunc.getList()) {
                            BasicBlock bb = bbNode.getValue();

                            if (bb != inlineFunc.getList().getFirst().getValue() && bb != retBlock) {
                                for (MyNode<Instr> instrNode :
                                        bb.getList()) {
                                    Instr instr = instrNode.getValue();
                                    // 把跳转到被内联函数出口块的指令重定向到splitBlock
                                    instr.replaceUseWith(retBlock, splitBlock);
                                    // 把对被内联函数入口块的使用替换成对callInstr所在block的使用
                                    instr.replaceUseWith(inlineFunc.getList().getFirst().getValue(), callInstr.bbBelongTo);
                                }
                                t.add(bb);
                            }
                        }
                        for (BasicBlock bb :
                                t) {
                            callInstr.bbBelongTo.getNode().insertAfter(bb.getNode());
                        }
                        subsCalls.add(callInstr);
                        //callInstr.removeMeFromAllMyUses();
                        //callInstr.getNode().removeMe();
                        ret.removeMeFromAllMyUses();
                        ret.getNode().removeMe();
                    }
                }
                subsCalls.forEach(instr -> {instr.removeMeFromAllMyUses();instr.getNode().removeMe();});
                inlinedFunctions.add(function.getNode());
            }

        });
        compUnit.fullMaintain();
        /*for (MyNode<Function> funcNode :
                inlinedFunctions) {
            if (funcNode.getValue().getUsers().isEmpty()) {
                funcNode.removeMe();
            }
        }*/
    }
    private RetInstr findRet(Function function) {
        // ret 大概率在最后一个基本块
        BasicBlock lastbb = function.getList().getLast().getValue();
        for (util.MyNode<ir.instruction.Instr> instrNode:
                lastbb.getList()) {
            Instr instr = instrNode.getValue();
            if (instr instanceof RetInstr) {
                return (RetInstr) instr;
            }
        }
        for (util.MyNode<BasicBlock> bbNode :
                function.getList()) {
            BasicBlock bb = bbNode.getValue();
            for (util.MyNode<Instr> instrNode:
                    bb.getList()) {
                Instr instr = instrNode.getValue();
                if (instr instanceof RetInstr) {
                    return (RetInstr) instr;
                }
            }
        }
        assert false;
        return null;
    }
    private boolean canInline(Function function) {
        if (!function.isRecursive() && !function.getName().equals("main")) {
            int length  = 0;
            for (util.MyNode<ir.value.BasicBlock> bbNode:
                    function.getList()) {
                ir.value.BasicBlock bb = bbNode.getValue();
                length += bb.getList().getSize();

            }
            return length <= AyaConfig.MAX_INLINE_LENGTH ||
                    function.getUsers().size() == 1 && AyaConfig.AGGRESSIVE_INLINE;
        }
        return false;
    }
}

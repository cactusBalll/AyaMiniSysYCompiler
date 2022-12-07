package ir;

import ir.instruction.Instr;
import ir.value.*;
import util.MyList;
import util.MyNode;

import java.lang.reflect.Constructor;
import java.util.HashMap;
import java.util.Map;

public class IRCloner {
    private static final IRCloner instance = new IRCloner();

    public static IRCloner getInstance() {
        return instance;
    }

    /**
     * 复制整个函数，并维护def-use关系，包括：
     * function的params，里面的basicBlocks，再里面的instruction。
     * treeir->llvmir的信息没有维护，支配信息没有维护（如需要，可用Analyze重新算）
     * 表示常数的InitVal没有复制（常数应该不可变吧（如果某个Pass改了就寄了）），但是表示函数参数的InitVal复制了。
     * @param function 被复制的函数
     * @return 复制的结果
     */
    public Function cloneFunction(Function function) {
        Function newFunc = new Function(function);
        Map<Value, Value> mapper = new HashMap<>();
        MyList<BasicBlock> newBBList = new MyList<>();
        for (MyNode<BasicBlock> bbNode :
                function.getList()) {
            BasicBlock bb = bbNode.getValue();
            BasicBlock newBB = new BasicBlock(bb);
            newBBList.add(newBB.getNode());
            mapper.put(bb, newBB);
        }
        newFunc.setList(newBBList);

        for (MyNode<BasicBlock> bbNode:
                newFunc.getList()) {
            BasicBlock bb = bbNode.getValue();
            MyList<Instr> newInstrList = new MyList<Instr>();
            for (MyNode<Instr> instrNode:
                    bb.getList()){
                Instr instr = instrNode.getValue();
                Instr newInstr = null;
                try {
                    Constructor<? extends Instr> constructor = instr.getClass().getConstructor(instr.getClass());
                    newInstr = constructor.newInstance(instr);
                    newInstr.bbBelongTo = bb;
                    mapper.put(instr, newInstr);
                } catch (Exception e) {
                    assert false;
                }
                newInstrList.addLast(newInstr.getNode());
            }
            bb.setList(newInstrList);
        }
        newFunc.getParams().clear();
        for (Param param :
                function.getParams()) {
            Param newParam = new Param( param);
            newParam.setName(param.getName());
            newFunc.getParams().add(newParam);
            mapper.put(param, newParam);
        }
        for (MyNode<BasicBlock> bbNode :
                newFunc.getList()) {
            BasicBlock bb = bbNode.getValue();
            for (MyNode<Instr> instrNode :
                    bb.getList()) {
                Instr instr = instrNode.getValue();
                for (Map.Entry<Value, Value> pair :
                        mapper.entrySet()) {
                    instr.replaceUseWith(pair.getKey(),pair.getValue());
                }
            }
        }
        return newFunc;
    }
}

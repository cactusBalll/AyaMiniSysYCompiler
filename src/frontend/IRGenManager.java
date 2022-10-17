package frontend;

import ir.instruction.*;
import ir.value.*;
import ty.IntArrTy;
import ty.Ty;

import java.util.ArrayList;
import java.util.List;

public class IRGenManager {
    private final CompUnit compUnit = new CompUnit();

    private Function nwFunction = null;

    private BasicBlock nwBlock = null;

    private BasicBlock retBB = null; // 函数出口块

    private Value retAlloc = null; // 返回值变量

    public CompUnit getCompUnit() {
        return compUnit;
    }

    public Function intoFunction(String name, List<Param> params, Ty ty) {
        Function function = new Function(ty,name,params);
        nwFunction = function;
        nwBlock = function.getFirstBB();
        return function;
    }

    public BuiltinCallInstr genInput() {
        BuiltinCallInstr builtin = new BuiltinCallInstr(BuiltinCallInstr.Func.GetInt, InitVal.buildInitVal(0));
        nwBlock.addInstr(builtin);
        return builtin;
    }

    public BuiltinCallInstr genPutStr(String str) {
        Value v = new MyString(str);
        BuiltinCallInstr builtin = new BuiltinCallInstr(BuiltinCallInstr.Func.PutStr, v);
        nwBlock.addInstr(builtin);
        return builtin;
    }

    public BuiltinCallInstr genPutInt(Value v) {
        BuiltinCallInstr builtin = new BuiltinCallInstr(BuiltinCallInstr.Func.PutInt, v);
        nwBlock.addInstr(builtin);
        return builtin;
    }
    public AllocInstr genStaticData(Ty type, Constant initVal, String name) {
        AllocInstr allocInstr = new AllocInstr(type, AllocInstr.AllocType.Static, initVal);
        allocInstr.setName(name);
        compUnit.addGlobalValue(allocInstr);
        return allocInstr;
    }

    public AllocInstr genStackData(Ty type, Value value) {
        AllocInstr allocInstr = new AllocInstr(type, AllocInstr.AllocType.Stack, null);
        StoreInstr init = new StoreInstr(allocInstr, value, InitVal.buildInitVal(0));
        nwFunction.getFirstBB().addInstr(allocInstr);
        nwBlock.addInstr(init);
        return allocInstr;
    }
    public AllocInstr genStackDataNoInit(Ty type) {
        AllocInstr allocInstr = new AllocInstr(type, AllocInstr.AllocType.Stack, null);
        nwFunction.getFirstBB().addInstr(allocInstr);
        return allocInstr;
    }
    public AllocInstr genStackData(Ty type, List<Value> value) {
        AllocInstr allocInstr = new AllocInstr(type, AllocInstr.AllocType.Stack, null);
        nwFunction.getFirstBB().addInstr(allocInstr);
        assert type instanceof IntArrTy;
        IntArrTy intArrTy = (IntArrTy) type;
        if (intArrTy.getDims().size() == 1) {
            for (int j = 0; j < value.size(); j++) {
                Value i = value.get(j);
                StoreInstr init = new StoreInstr(allocInstr, i, InitVal.buildInitVal(j));
                nwBlock.addInstr(init);
            }
        } else if (intArrTy.getDims().size() == 2){
            for (int j = 0; j < intArrTy.getDims().get(0); j++) {
                for (int k = 0; k < intArrTy.getDims().get(1); k++) {
                    int idx = j * intArrTy.getDims().get(1) + k;
                    Value i = value.get(idx);
                    StoreInstr init = new StoreInstr(allocInstr, i, InitVal.buildInitVal(idx));
                    nwBlock.addInstr(init);
                }
            }
        }

        return allocInstr;
    }
    public BinaryOp genBinaryOp(BinaryOp.OpType type, Value left, Value right) {
        BinaryOp binaryOp = new BinaryOp(type, left, right);
        nwBlock.addInstr(binaryOp);
        return binaryOp;
    }

    public CallInstr genCallInstr(Function function, List<Value> params) {
        CallInstr callInstr = new CallInstr(function, params);
        nwBlock.addInstr(callInstr);
        return callInstr;
    }

    public LoadInstr genLoadInstr(Value ptr, Value idx) {
        LoadInstr loadInstr = new LoadInstr(ptr, idx);
        nwBlock.addInstr(loadInstr);
        return loadInstr;
    }

    public ArrView genArrView(Value ptr, Value idx) {
        ArrView arrView = new ArrView(ptr, idx);
        nwBlock.addInstr(arrView);
        return arrView;
    }
    public StoreInstr genStoreInstr(Value ptr, Value idx, Value target) {
        StoreInstr storeInstr = new StoreInstr(ptr, idx, target);
        nwBlock.addInstr(storeInstr);
        return storeInstr;
    }

    public Value genIndex(Value instr, Value i, Value j) {
        Value offset;
        if (instr instanceof  AllocInstr) {
            AllocInstr allocInstr = (AllocInstr)instr;
            offset = InitVal.buildInitVal(((IntArrTy) allocInstr.getAllocTy()).getDims().get(1));
        } else if (instr instanceof Param){
            Param param = (Param) instr;
            offset = InitVal.buildInitVal(((IntArrTy) param.getType()).getDims().get(1));
        } else {
            offset = InitVal.buildInitVal(0);
        }

        Instr instr0 = new BinaryOp(BinaryOp.OpType.Mul, i, offset);
        Instr instr1 = new BinaryOp(BinaryOp.OpType.Add, instr0, j);

        nwBlock.addInstr(instr0);
        nwBlock.addInstr(instr1);
        return instr1;
    }

    public BasicBlock getBB() {
        BasicBlock bb = new BasicBlock();
        nwFunction.getList().add(bb.getNode());
        nwBlock = bb;
        return bb;
    }

    public void intoBB(BasicBlock bb) {
        nwBlock = bb;
    }

    static class LoopCtx{
        private final BasicBlock exitBB;
        private final BasicBlock bodyBB;

        public LoopCtx(BasicBlock exitBB, BasicBlock bodyBB) {
            this.exitBB = exitBB;
            this.bodyBB = bodyBB;
        }

        public BasicBlock getBodyBB() {
            return bodyBB;
        }

        public BasicBlock getExitBB() {
            return exitBB;
        }
    }
}

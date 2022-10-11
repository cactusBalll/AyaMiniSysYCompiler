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

    public CompUnit getCompUnit() {
        return compUnit;
    }

    public AllocInstr genStaticData(Ty type, Constant initVal) {
        AllocInstr allocInstr = new AllocInstr(type, AllocInstr.AllocType.Static, initVal);
        compUnit.addGlobalValue(allocInstr);
        return allocInstr;
    }

    public AllocInstr genStackData(Ty type, int value) {
        AllocInstr allocInstr = new AllocInstr(type, AllocInstr.AllocType.Stack, null);
        StoreInstr init = new StoreInstr(allocInstr, InitVal.buildInitVal(value), new ArrayList<>());
        nwFunction.getFirstBB().addInstr(allocInstr);
        nwBlock.addInstr(init);
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

    public LoadInstr genLoadInstr(Value ptr, List<Value> idx) {
        LoadInstr loadInstr = new LoadInstr(ptr, idx);
        nwBlock.addInstr(loadInstr);
        return loadInstr;
    }

}

package ir.instruction;

import ir.value.Constant;
import ir.value.User;
import ir.value.Value;
import ty.IntTy;
import ty.Ty;
import util.MyNode;

import java.util.ArrayList;
import java.util.LinkedList;

public class AllocInstr extends Instr{

    public enum AllocType{
        Static,
        Stack,
    }

    private AllocType allocType;
    private Ty allocTy;

    private Constant initVal; //静态变量的初始值，要放到.data
    // 栈上数组初始值应该翻译为一系列store
    // const数组都应放在.data

    public AllocType getAllocType() {
        return allocType;
    }

    public Ty getAllocTy() {
        return allocTy;
    }

    public Constant getInitVal() {
        return initVal;
    }
    private static AllocInstr nullPtr = new AllocInstr(IntTy.build(false), AllocType.Static, null);
    public static AllocInstr getNullPtr() {
        return nullPtr;
    }
    public AllocInstr(Ty allocTy, AllocType allocType, Constant initVal) {
        super(new LinkedList<>(), new ArrayList<>());
        this.allocType = allocType;
        this.allocTy = allocTy;
        this.initVal = initVal;
    }
    public AllocInstr(LinkedList<MyNode<User>> users, ArrayList<Value> uses) {
        super(users, uses);
    }

    public AllocInstr(LinkedList<MyNode<User>> users, Ty type) {
        super(users, type);
    }

    public AllocInstr(User rhs) {
        super(rhs);
    }

    public AllocInstr(Ty type, String name) {
        super(type, name);
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        if (allocType == AllocType.Static) {
            sb.append('%');
        }
        sb.append(name).append(" = ");
        sb.append("alloc").append(' ').append(allocType).append(' ').append(allocTy);
        if (allocType == AllocType.Static) {
            sb.append(" := ").append(initVal);
        }

        return sb.toString();
    }
}

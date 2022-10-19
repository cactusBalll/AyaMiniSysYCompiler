package ir.instruction;

import ir.value.User;
import ir.value.Value;
import ty.Ty;
import util.MyNode;

import java.util.ArrayList;
import java.util.LinkedList;

/**
 * ArrView: IntArrTy -> IntArrTy
 * 处理部分数组传值
 * 实际上是一个计算指令，也就是说可以GVNGCM
 */
public class ArrView extends Instr{

    public ArrView(Value arr,Value idx) {
        super(new LinkedList<>(), new ArrayList<>());
        uses.add(arr.getNode());
        uses.add(idx.getNode());
    }

    public Value getArr() {
        return uses.get(0).getValue();
    }

    public Value getIdx() {
        return uses.get(1).getValue();
    }
    public ArrView(LinkedList<MyNode<User>> users, ArrayList<Value> uses) {
        super(users, uses);
    }

    public ArrView(LinkedList<MyNode<User>> users, Ty type) {
        super(users, type);
    }

    public ArrView(User rhs) {
        super(rhs);
    }

    public ArrView(Ty type, String name) {
        super(type, name);
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append(name).append(" = ");
        sb.append("arrView ").append(getArr().getName()).append(',').append(getIdx().getName());
        return sb.toString();
    }
}

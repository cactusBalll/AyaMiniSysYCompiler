package ir.instruction;

import ir.value.User;
import ir.value.Value;
import ty.Ty;
import util.MyNode;

import java.util.ArrayList;
import java.util.LinkedList;

public class BinaryOp extends Instr{
    private OpType opType;

    public OpType getOpType() {
        return opType;
    }

    public Value getLeft() {
        return uses.get(0).getValue();
    }

    public Value getRight() {
        return uses.get(1).getValue();
    }
    public enum OpType{
        Add,
        Sub,
        Mul,
        Div,
        Mod,
        Slt,
        Sle,
        Sgt,
        Sge,
        Seq,
        Sne,
        Nor, // MIPS用NOR实现非
    }
    public BinaryOp(OpType opType, Value left, Value right) {
        super(new LinkedList<>(), new ArrayList<>());
        this.opType = opType;
        uses.add(left.getNode());
        uses.add(right.getNode());
    }
    public BinaryOp(LinkedList<MyNode<User>> users, ArrayList<Value> uses) {
        super(users, uses);
    }

    public BinaryOp(LinkedList<MyNode<User>> users, Ty type) {
        super(users, type);
    }

    public BinaryOp(User rhs) {
        super(rhs);
    }

    public BinaryOp(Ty type, String name) {
        super(type, name);
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append(name).append(" = ");
        sb.append(opType.toString().toLowerCase())
                .append(' ').append(getLeft().getName())
                .append(',').append(getRight().getName());
        return sb.toString();
    }
}

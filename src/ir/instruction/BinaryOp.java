package ir.instruction;

import ir.value.InitVal;
import ir.value.User;
import ir.value.Value;
import ty.Ty;
import util.MyNode;

import java.util.ArrayList;
import java.util.LinkedList;
import java.util.Objects;

public class BinaryOp extends Instr {
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

    public enum OpType {
        Add,
        Sub,
        Mul,
        Div,
        Mod,
        Slt, // 感觉逻辑运算的语义可以当作0和非0，和C一样，这样后端似乎生成代码质量更高
        Sle,
        Sgt,
        Sge,
        Seq,
        Sne,
        Not, // MIPS用XOR实现非
    }
    public boolean isCondSet() {
        //return opType == OpType.Seq || opType == OpType.Sne /*|| opType == OpType.Sle ||
        //        opType == OpType.Slt || opType == OpType.Sge || opType == OpType.Sgt*/ || opType == OpType.Not;
        return false;
    }
    public Integer evaluate() {
        if (!(getLeft() instanceof InitVal) || !(getRight() instanceof InitVal)) {
            return null;
        }
        int left = ((InitVal) getLeft()).getValue();
        int right = ((InitVal) getRight()).getValue();
        switch (opType) {
            case Slt:
                return left < right ? 1 : 0;
            case Not:
                return right != 0 ? 0 : 1;
            case Sle:
                return left <= right ? 1 : 0;
            case Sge:
                return left >= right ? 1 : 0;
            case Sne:
                return left != right ? 1 : 0;
            case Seq:
                return left == right ? 1 : 0;
            case Mod:
                return left % right;
            case Div:
                return left / right;
            case Mul:
                return left * right;
            case Sub:
                return left - right;
            case Add:
                return left + right;
            case Sgt:
                return left > right ? 1 : 0;
        }
        return null;
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

    public class Wrapper{
        private BinaryOp owner;

        public Wrapper(BinaryOp owner) {
            this.owner = owner;
        }

        @Override
        public int hashCode() {
            return Objects.hash(getLeft(),getRight(),opType);
        }

        @Override
        public boolean equals(Object obj) {
            if (!(obj instanceof Wrapper)) {
                return false;
            }
            Wrapper wrapper = (Wrapper) obj;
            if (owner.isCondSet() || wrapper.owner.isCondSet()) {
                return false;
            }
            return Objects.equals(owner.getLeft(),wrapper.owner.getLeft()) &&
                    Objects.equals(owner.getRight(),wrapper.owner.getRight()) &&
                    Objects.equals(owner.getOpType(), wrapper.owner.getOpType());
        }
    }

    private final Wrapper wrapper = new Wrapper(this);

    public Wrapper getWrapper() {
        return wrapper;
    }
}

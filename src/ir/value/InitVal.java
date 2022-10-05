package ir.value;

import ty.Ty;
import util.MyNode;

import java.util.*;

/**
 * 表示立即数
 */
public class InitVal extends Value{

    private static HashMap<Integer, InitVal> allocatedInitVal = new HashMap<>();

    private int value;

    private InitVal(int value) {
        super(new LinkedList<>());
        this.value = value;
    }

    public int getValue() {
        return value;
    }

    // 保证相同值的InitVal指向同一个实例
    public InitVal buildInitVal(int value) {
        if (allocatedInitVal.containsKey(value)) {
            return allocatedInitVal.get(value);
        } else {
            InitVal ret = new InitVal(value);
            allocatedInitVal.put(value, ret);
            return ret;
        }
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        InitVal initVal = (InitVal) o;
        return value == initVal.value;
    }

    @Override
    public int hashCode() {
        return Objects.hash(value);
    }

    public InitVal(LinkedList<MyNode<User>> users, Ty type) {
        super(users, type);
    }

    public InitVal(Value rhs) {
        super(rhs);
    }

    public InitVal(Ty type, String name) {
        super(type, name);
    }

    public InitVal(Ty type) {
        super(type);
    }

    public InitVal(LinkedList<MyNode<User>> users) {
        super(users);
    }
}

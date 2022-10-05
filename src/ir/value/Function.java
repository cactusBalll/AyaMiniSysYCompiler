package ir.value;

import ty.Ty;
import util.MyList;
import util.MyNode;

import java.util.ArrayList;
import java.util.LinkedList;

public class Function extends User{

    private final MyList<BasicBlock> list = new MyList<>();

    public MyList<BasicBlock> getList() {
        return list;
    }

    public Function(LinkedList<MyNode<User>> users, ArrayList<Value> uses) {
        super(users, uses);
    }

    public Function(LinkedList<MyNode<User>> users, Ty type) {
        super(users, type);
    }

    public Function(User rhs) {
        super(rhs);
    }

    public Function(Ty type, String name) {
        super(type, name);
    }
}

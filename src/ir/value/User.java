package ir.value;

import ty.Ty;
import util.MyNode;

import java.util.ArrayList;
import java.util.LinkedList;

public abstract class User extends Value{
    protected LinkedList<MyNode<Value>> uses; //使用的值

    public User(LinkedList<MyNode<User>> users, ArrayList<Value> uses) {
        super(users);
        this.uses = new LinkedList<>();
    }

    public LinkedList<MyNode<Value>> getUses() {
        return uses;
    }

    public void replaceUseWith(Value old, Value nnew) {
        // 默认实现使用uses列表
        /*if (uses.remove(old.getNode())) {
            uses.add(nnew.getNode()); // 想办法绕过这个类型检查
        }*/
        if (uses.contains(old.getNode())) {
            old.users.remove(this.getNode());
            nnew.users.add((MyNode<User>) this.getNode());
            uses.remove(old.getNode());
            uses.add(nnew.getNode());
        }
    }
    public void removeMeFromAllMyUses() {
        for (MyNode<Value> vNode :
                uses) {
            Value v = vNode.getValue();
            v.getUsers().remove(this.getNode());
        }
    }



    public User(LinkedList<MyNode<User>> users, Ty type) {
        super(users, type);
        this.uses = new LinkedList<>();
    }

    public User(User rhs) {
        super(rhs);
        this.uses = (LinkedList<MyNode<Value>>) rhs.uses.clone();
    }

    public void addUse(Value value) {
        uses.add(value.getNode());
    }

    public void addUse(Value value, int index) {
        if (uses.isEmpty()) {
            uses.add(value.getNode());
            return;
        }
        uses.set(index, value.getNode());
    }

    public User(Ty type, String name) {
        super(type, name);
        uses = new LinkedList<>();
    }

    //不删除old节点，只进行替换
    public void replaceUse(Value old, Value nnew) {
        if (uses.contains(old.getNode())) {
            old.users.remove(this.getNode());
            nnew.users.add((MyNode<User>) this.getNode());
            uses.remove(old.getNode());
            uses.add(nnew.getNode());
        }
    }
}

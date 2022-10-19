package ir.instruction;

import ir.value.Function;
import ir.value.User;
import ir.value.Value;
import ty.Ty;
import util.MyNode;

import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;

public class CallInstr extends Instr{

    public CallInstr(Function function, List<Value> params) {
        super(new LinkedList<>(), new ArrayList<>());
        this.uses.add(function.getNode());
        for (Value v :
                params) {
            this.uses.add(v.getNode());
        }
    }

    public Function getFunction() {
        return (Function) uses.getFirst().getValue();
    }

    public List<Value> getParams() {
        List<Value> params = new ArrayList<>();
        for (int i = 1; i < uses.size(); i++) {
            params.add(uses.get(i).getValue());
        }
        return params;
    }

    public CallInstr(LinkedList<MyNode<User>> users, ArrayList<Value> uses) {
        super(users, uses);
    }

    public CallInstr(LinkedList<MyNode<User>> users, Ty type) {
        super(users, type);
    }

    public CallInstr(User rhs) {
        super(rhs);
    }

    public CallInstr(Ty type, String name) {
        super(type, name);
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append(name).append(" = ");
        sb.append(getFunction().getName()).append('(');
        for (Value v :
                getParams()) {
            sb.append(v.getName()).append(',');
        }
        sb.append(')');
        return sb.toString();
    }

}

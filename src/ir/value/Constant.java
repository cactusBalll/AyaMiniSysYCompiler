package ir.value;

import ty.Ty;

import java.util.List;

public class Constant {
    private final Ty ty;
    private final int value;
    private final List<Integer> listValue;

    public Constant(Ty ty, List<Integer> listValue) {
        this.ty = ty;
        this.listValue = listValue;
        this.value = 0;
    }

    public Constant(Ty ty, int value) {
        this.ty = ty;
        this.listValue = null;
        this.value = value;
    }

    public Ty getTy() {
        return ty;
    }

    public int getValue() {
        return value;
    }

    public List<Integer> getListValue() {
        return listValue;
    }
}

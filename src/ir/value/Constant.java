package ir.value;

import ty.IntArrTy;
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

    public int getValue(int idx1, int idx2) {
        if (!(ty instanceof IntArrTy)) {
            return 0;
        } else {
            IntArrTy intArrTy = (IntArrTy) ty;
            if (intArrTy.getDims().size() == 2) {
                return listValue.get(intArrTy.getDims().get(1) * idx1 + idx2);
            } else {
                return listValue.get(idx1);
            }
        }
    }
}

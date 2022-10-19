package ir.value;

import ty.IntArrTy;
import ty.Ty;

import java.util.List;

public class Constant {
    private final Ty ty;
    private final int value;
    private final List<Integer> listValue;

    private final boolean zeroInitialized;

    public Constant(Ty ty, List<Integer> listValue) {
        this.ty = ty;
        this.listValue = listValue;
        this.value = 0;
        zeroInitialized = false;
    }

    public Constant(Ty ty, int value) {
        this.ty = ty;
        this.listValue = null;
        this.value = value;
        zeroInitialized = false;
    }

    public Constant(Ty ty) {
        this.ty = ty;
        this.listValue = null;
        this.value = 0;
        zeroInitialized = true;
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
        if (zeroInitialized) {
            return 0;
        }
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

    @Override
    public String toString() {
        if (listValue != null) {
            StringBuilder sb = new StringBuilder();
            sb.append('[');
            for (int v :
                    listValue) {
                sb.append(v).append(',');
            }
            sb.append(']');
            return sb.toString();
        } else if (zeroInitialized){
            return "zeroInit";
        } else {
            return String.valueOf(value);
        }
    }
}

package ty;

import java.util.ArrayList;
import java.util.List;

public class IntArrTy extends Ty{
    public static final int ANY_DIM = -1;
    private final List<Integer> dims;

    public List<Integer> getDims() {
        return dims;
    }

    private IntArrTy(List<Integer> dims, boolean isConst) {
        super(isConst);
        this.dims = dims;
    }


    public static IntArrTy build(int d0, boolean isConst) {
        List<Integer> t = new ArrayList<>();
        t.add(d0);
        return new IntArrTy(t, isConst);
    }

    public static IntArrTy build(int d0, int d1, boolean isConst) {
        List<Integer> t = new ArrayList<>();
        t.add(d0);
        t.add(d1);
        return new IntArrTy(t, isConst);
    }
}

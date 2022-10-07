package ty;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

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

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        IntArrTy intArrTy = (IntArrTy) o;
        return Objects.equals(dims, intArrTy.dims);
    }

    @Override
    public int hashCode() {
        return Objects.hash(dims);
    }
}

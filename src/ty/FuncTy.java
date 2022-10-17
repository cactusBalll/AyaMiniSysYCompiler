package ty;

import java.util.List;
import java.util.Objects;

public class FuncTy extends Ty{
    private final Ty ret;
    private final List<Ty> params;

    private FuncTy(Ty ret, List<Ty> params) {
        super(false);
        this.ret = ret;
        this.params = params;
    }

    public static FuncTy build(Ty ret, List<Ty> params) {
        return new FuncTy(ret, params);
    }

    public List<Ty> getParams() {
        return params;
    }

    public Ty getRet() {
        return ret;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        FuncTy funcTy = (FuncTy) o;
        return Objects.equals(ret, funcTy.ret) && Objects.equals(params, funcTy.params);
    }

    @Override
    public int hashCode() {
        return Objects.hash(ret, params);
    }
}

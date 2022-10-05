package ty;

import java.util.List;

public class FuncTy extends Ty{
    private final Ty ret;
    private final List<Ty> params;

    private FuncTy(Ty ret, List<Ty> params) {
        super(false);
        this.ret = ret;
        this.params = params;
    }

    public FuncTy build(Ty ret, List<Ty> params) {
        return new FuncTy(ret, params);
    }

    public List<Ty> getParams() {
        return params;
    }

    public Ty getRet() {
        return ret;
    }
}

package ty;

public class PointerTy extends Ty{
    private final Ty inner;
    private PointerTy(Ty inner, boolean isConst) {
        super(isConst);
        this.inner = inner;
    }

    public static PointerTy build(Ty inner) {
        return new PointerTy(inner,false);
    }

    public Ty getInner() {
        return inner;
    }
}

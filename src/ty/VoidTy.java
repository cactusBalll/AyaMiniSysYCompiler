package ty;

public class VoidTy extends Ty{
    private static final VoidTy instance = new VoidTy(false);

    public static VoidTy build() {
        return instance;
    }

    public VoidTy(boolean isConst) {
        super(isConst);
    }
}

package ty;

public class VoidTy extends Ty{
    private static final VoidTy instance = new VoidTy(false);

    public static VoidTy build() {
        return instance;
    }

    public VoidTy(boolean isConst) {
        super(isConst);
    }

    @Override
    public boolean equals(Object obj) {
        return obj instanceof VoidTy;
    }

    @Override
    public String toString() {
        return "void";
    }
}

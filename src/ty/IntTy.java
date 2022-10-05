package ty;

public class IntTy extends Ty{
    private static final IntTy instance = new IntTy(false);
    private static final IntTy constInstance = new IntTy(true);

    public IntTy(boolean isConst) {
        super(isConst);
    }

    public static IntTy build(boolean isConst) {
        if (isConst) {
            return constInstance;
        } else {
            return instance;
        }
    }

}

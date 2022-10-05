package ty;

/**
 * 占位
 */
public class NullTy extends Ty{
    private static final NullTy instance = new NullTy(false);

    public NullTy(boolean isConst) {
        super(isConst);
    }

    public static NullTy build() {
        return instance;
    }
}

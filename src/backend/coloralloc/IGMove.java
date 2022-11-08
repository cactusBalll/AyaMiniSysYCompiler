package backend.coloralloc;

/**
 * 冲突图中的move
 */
public class IGMove {
    public IGNode from;
    public IGNode to;

    public IGMove(IGNode from, IGNode to) {
        this.from = from;
        this.to = to;
    }
}

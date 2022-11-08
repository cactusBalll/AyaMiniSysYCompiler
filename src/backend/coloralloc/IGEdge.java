package backend.coloralloc;

import java.util.Objects;

public class IGEdge {
    public IGNode u;
    public IGNode v;

    public IGEdge(IGNode u, IGNode v) {
        this.u = u;
        this.v = v;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        IGEdge igEdge = (IGEdge) o;
        return Objects.equals(u, igEdge.u) && Objects.equals(v, igEdge.v);
    }

    @Override
    public int hashCode() {
        return Objects.hash(u, v);
    }
}

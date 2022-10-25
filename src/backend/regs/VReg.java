package backend.regs;

public class VReg extends Reg{
    public int id;

    private static int allocateCounter = 0;

    public static VReg alloc() {
        VReg ret = new VReg(allocateCounter);
        allocateCounter++;
        return ret;
    }

    public static void resetCnt() {
        allocateCounter = 0;
    }
    public VReg(int id) {
        this.id = id;
    }

    @Override
    public String toString() {
        return "$v" + id;
    }
}

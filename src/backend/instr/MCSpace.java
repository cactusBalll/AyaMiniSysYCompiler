package backend.instr;

public class MCSpace extends MCData{
    public int sizeInWord;

    public MCSpace(Label label, int sizeInWord) {
        super(label);
        this.sizeInWord = sizeInWord;
    }

    @Override
    public String toString() {
        return String.format("%s: .space %d", label, sizeInWord * 4);
    }
}

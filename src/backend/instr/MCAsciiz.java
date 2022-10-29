package backend.instr;

public class MCAsciiz extends MCData{
    public String string;

    public MCAsciiz(Label label, String string) {
        super(label);
        this.string = string;
    }

    @Override
    public String toString() {
        return String.format("%s: .asciiz \"%s\"", label.name, string);
    }
}

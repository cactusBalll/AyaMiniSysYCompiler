package backend.instr;

import java.util.List;

public class MCWord extends MCData{
    public List<Integer> words;
    public MCWord(Label label, List<Integer> words) {
        super(label);
        this.words = words;
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append(label.name).append(" .word ");
        for (int word :
                words) {
            sb.append(word).append(',');
        }
        sb.deleteCharAt(sb.length()-1);
        return sb.toString();
    }
}

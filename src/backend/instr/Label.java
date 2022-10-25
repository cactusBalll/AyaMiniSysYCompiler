package backend.instr;


import backend.MCBlock;

public class Label {
    public String name;
    private MCBlock owner;

    private static int counter = 0;

    public static Label getLabel(MCBlock owner) {
        Label label = new Label("BB"+counter, owner);
        counter++;
        return label;
    }

    public Label(String name, MCBlock owner) {
        this.name = name;
        this.owner = owner;
    }

    @Override
    public String toString() {
        return name + ':';
    }

    public MCBlock getOwner() {
        return owner;
    }

    public void setOwner(MCBlock owner) {
        this.owner = owner;
    }
}

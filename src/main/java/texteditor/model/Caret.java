package texteditor.model;

public class Caret {
    public enum Affinity {LEFT, RIGHT}

    private int position;
    private final PieceTable document;
    private Affinity affinity;

    public Caret(PieceTable document) {
        this.document = document;
        this.position = 0;
        this.affinity = Affinity.RIGHT;
    }

    public int getPosition() {return position;}

    public void setPosition(int position) {
        int max = document.getLength();
        this.position = Math.max(0, Math.min(position, max));
    }

    public Affinity getAffinity() {return affinity;}
    public void setAffinity(Affinity affinity) {this.affinity = affinity;}

}

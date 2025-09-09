package texteditor.model;

public class CursorModel {
    public enum Affinity {LEFT, RIGHT}

    private int position;
    private final PieceTable document;
    private Affinity affinity;

    public CursorModel(PieceTable document) {
        this.document = document;
        this.position = 0;
        this.affinity = affinity.RIGHT;
    }

    public int getPosition() {return position;}

    public void setPosition(int position) {
        if (position >= 0 && position <= document.getDocumentLength()) {this.position = position;}
    }

    public Affinity getAffinity() {return affinity;}
    public void setAffinity(Affinity affinity) {this.affinity = affinity;}

}

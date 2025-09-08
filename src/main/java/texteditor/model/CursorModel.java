package texteditor.model;

import texteditor.view.EditorCanvas;
import texteditor.view.layout.VisualLine;

import java.util.List;

public class CursorModel {
    private int position;
    private final PieceTable document;
    private final EditorCanvas canvas;
    public enum Affinity {LEFT, RIGHT}

    private Affinity affinity;

    public CursorModel(PieceTable document, EditorCanvas canvas) {
        this.document = document;
        this.canvas = canvas;
        this.position = 0;
        this.affinity = affinity.RIGHT;
    }

    public int getPosition() {
        return position;
    }

    public void setPosition(int position) {
        if (position >= 0 && position <= document.getDocumentLength()) {
            this.position = position;
        }
    }

    public Affinity getAffinity() {return affinity;}
    public void setAffinity(Affinity affinity) {this.affinity = affinity;}

}

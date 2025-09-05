package texteditor.model;

import texteditor.view.EditorCanvas;
import texteditor.view.EditorCanvas.VisualLine;

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

    public void moveRight() {
        setPosition(position + 1);
    }
    public void moveLeft() {
        setPosition(position - 1);
    }

    public void moveDown() {
        List<VisualLine> lines = canvas.getVisualLines();
        if (lines.isEmpty()) return;

        int currentIndex = canvas.findVisualLineIndexForPosition(position);
        if (currentIndex == -1 || currentIndex >= lines.size() - 1) return;

        VisualLine curLine = lines.get(currentIndex);
        int currentCol = Math.min(position - curLine.startPosition(), curLine.length());

        VisualLine nextLine = lines.get(currentIndex + 1);
        int targetCol = Math.min(currentCol, nextLine.length());

        setPosition(nextLine.startPosition() + targetCol);
    }

    public void moveUp() {
        List<VisualLine> lines = canvas.getVisualLines();
        if (lines.isEmpty()) return;

        int currentIndex = canvas.findVisualLineIndexForPosition(position);
        if (currentIndex <= 0) return;

        VisualLine curLine = lines.get(currentIndex);
        int currentCol = Math.min(position - curLine.startPosition(), curLine.length());

        VisualLine prevLine = lines.get(currentIndex - 1);
        int targetCol = Math.min(currentCol, prevLine.length());

        setPosition(prevLine.startPosition() + targetCol);
    }


    public void moveEnd() {
        int lineIndex = canvas.getVisualLineIndex(position);
        setPosition(canvas.getVisualLineEndPosition(lineIndex));
    }

    public void moveHome() {
        int lineIndex = canvas.getVisualLineIndex(position);
        setPosition(canvas.getVisualLineStartPosition(lineIndex));
    }


}

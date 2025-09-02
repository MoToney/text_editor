package texteditor.model;

import texteditor.view.EditorCanvas;
import texteditor.view.EditorCanvas.VisualLine;

import java.util.List;

public class CursorModel {
    private int position;
    private final PieceTable document;
    private final EditorCanvas canvas;

    public CursorModel(PieceTable document, EditorCanvas canvas) {
        this.document = document;
        this.canvas = canvas;
        this.position = 0;
    }

    public int getPosition() {
        return position;
    }

    public void setPosition(int position) {
        if (position >= 0 && position <= document.getDocumentLength()) {
            this.position = position;
        }
    }

    public void moveRight() {
        setPosition(position + 1);
    }
    public void moveLeft() {
        setPosition(position - 1);
    }
    public void moveDown() {
        List<VisualLine> lines = canvas.getVisualLines();
        if (lines.isEmpty()) return;

        int currentVisualLineIndex = canvas.getVisualLineIndex(position);
        int currentColumn = canvas.getVisualLineColumn(currentVisualLineIndex, position);


        if (currentVisualLineIndex != -1 && currentVisualLineIndex < lines.size() - 1) {
            VisualLine nextLine = lines.get(currentVisualLineIndex + 1);

            int targetColumn = Math.min(currentColumn, nextLine.length() );

            setPosition(nextLine.startPosition() + targetColumn);
        }


        /*int currentColumnIndex = document.getColumnIndex(position);
        int currentLineIndex = document.getLineIndex(position);

        if (document.isLastLine(currentLineIndex)) return;

        int nextLineIndex = currentLineIndex + 1;

        int targetColumnIndex = (!document.isLastLine(nextLineIndex))
                ? Math.min(currentColumnIndex, document.getLineLength(nextLineIndex) -1 )
                : Math.min(currentColumnIndex, document.getLineLength(nextLineIndex));

        int newPosition = document.getPosition(nextLineIndex, targetColumnIndex);
        setPosition(newPosition);*/

    }

    public void moveUp() {
        int currentLineIndex = canvas.getVisualLineIndex(position);
        int currentColumnIndex = canvas.getVisualLineColumn(currentLineIndex, position);

        if (currentLineIndex <= 0) return;

        int previousLineIndex = currentLineIndex - 1;

        int targetColumnIndex = Math.min(currentColumnIndex, canvas.getVisualLineLength(previousLineIndex));

        int newPosition = canvas.getVisualPosition(previousLineIndex, targetColumnIndex);

        setPosition(newPosition);
    }

    public void moveEnd() {
        int lineIndex = document.getLineIndex(position);

        int newPosition = (document.isLastLine(lineIndex))
                ? document.getPositionAtEndOfLine(lineIndex)
                : document.getPositionAtEndOfLine(lineIndex) - 1;
        setPosition(newPosition);
    }

    public void moveHome() {
        int lineIndex = document.getLineIndex(position);

        int newPosition = document.getPositionAtStartOfLine(lineIndex);
        setPosition(newPosition);
    }


}

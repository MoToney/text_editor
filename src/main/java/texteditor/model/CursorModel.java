package texteditor.model;

import javafx.scene.canvas.GraphicsContext;
import javafx.scene.paint.Color;
import javafx.scene.text.Text;

public class CursorModel {
    private int position;
    private final PieceTable document;

    public CursorModel(PieceTable document) {
        this.document = document;
        this.position = 0;
    }

    public int getPosition() {
        return position;
    }

    public void setPosition(int position) {
        if (position >= 0 && position <= document.getLength()) {
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
        int currentColumnIndex = document.getColumnIndex(position);
        int currentLineIndex = document.getLineIndex(position);

        if (document.isLastLine(currentLineIndex)) return;

        int nextLineIndex = currentLineIndex + 1;

        int targetColumnIndex = (!document.isLastLine(nextLineIndex))
                ? Math.min(currentColumnIndex, document.getLineLength(nextLineIndex) -1 )
                : Math.min(currentColumnIndex, document.getLineLength(nextLineIndex));

        int newPosition = document.getPosition(nextLineIndex, targetColumnIndex);
        setPosition(newPosition);

    }

    public void moveUp() {
        int currentColumnIndex = document.getColumnIndex(position);
        int currentLineIndex = document.getLineIndex(position);

        if (currentLineIndex <= 0) return;

        int previousLineIndex = currentLineIndex - 1;

        int targetColumnIndex = (currentColumnIndex > document.getLineLength(previousLineIndex))
                ? document.getLineLength(previousLineIndex) - 1
                : currentColumnIndex;

        int newPosition = document.getPosition(previousLineIndex, targetColumnIndex);

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

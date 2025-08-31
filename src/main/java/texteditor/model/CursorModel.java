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
}

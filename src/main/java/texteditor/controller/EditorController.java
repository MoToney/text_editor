package texteditor.controller;

import javafx.scene.Scene;
import javafx.scene.input.KeyCode;
import texteditor.model.CursorModel;
import texteditor.model.PieceTable;
import texteditor.view.EditorCanvas;

public class EditorController {
    private final Scene scene;
    private final PieceTable document;
    private final CursorModel cursor;
    private final EditorCanvas canvas;

    public EditorController(Scene scene, PieceTable document, CursorModel cursor, EditorCanvas canvas) {
        this.scene = scene;
        this.document = document;
        this.cursor = cursor;
        this.canvas = canvas;

        setupKeyHandlers();
    }

    private void setupKeyHandlers() {
        scene.setOnKeyPressed(event -> {
            boolean modelChanged = false;

            if (event.getCode() == KeyCode.LEFT) {
                canvas.moveLeft();
            } else if (event.getCode() == KeyCode.RIGHT) {
                canvas.moveRight();
            } else if (event.getCode() == KeyCode.UP) {
                canvas.moveUp();
            } else if (event.getCode() == KeyCode.DOWN) {
                canvas.moveDown();
            } else if (event.getCode() == KeyCode.END) {
                canvas.moveEnd();
            } else if (event.getCode() == KeyCode.HOME) {
                canvas.moveHome();
            } else if (event.getCode() == KeyCode.ENTER) {
                document.insertText(cursor.getPosition(), "\n");
                canvas.moveRight();
            } else if (event.getCode().isLetterKey() || event.getCode().isDigitKey() || event.getText().length() == 1) {
                document.insertText(cursor.getPosition(), event.getText());
                canvas.moveRight();
            } else if (event.getCode() == KeyCode.BACK_SPACE) {
                document.removeText(cursor.getPosition() - 1, 1);
                canvas.moveLeft();
            }
            if (modelChanged) {
                canvas.resetCursorBlink();
                canvas.draw();
            }
        });
    }
}

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
                cursor.moveLeft();
                modelChanged = true;
            } else if (event.getCode() == KeyCode.RIGHT) {
                cursor.moveRight();
                modelChanged = true;
            } else if (event.getCode() == KeyCode.UP) {
                cursor.moveUp();
                modelChanged = true;
            } else if (event.getCode() == KeyCode.DOWN) {
                cursor.moveDown();
                modelChanged = true;
            } else if (event.getCode() == KeyCode.END) {
                cursor.moveEnd();
                modelChanged = true;

            } else if (event.getCode() == KeyCode.HOME) {
                cursor.moveHome();
                modelChanged = true;
            } else if (event.getCode().isLetterKey() || event.getCode().isDigitKey() || event.getText().length() == 1) {
                document.insertText(cursor.getPosition(), event.getText());
                cursor.moveRight();
                modelChanged = true;
            } else if (event.getCode() == KeyCode.BACK_SPACE) {
                document.removeText(cursor.getPosition() - 1, 1);
                cursor.moveLeft();
                modelChanged = true;
            }

            if (modelChanged) {
                canvas.resetCursorBlink();
                canvas.draw();
            }
        });
    }
}

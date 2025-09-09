package texteditor.controller;

import javafx.scene.Scene;
import javafx.scene.input.KeyCode;
import texteditor.model.Caret;
import texteditor.model.PieceTable;
import texteditor.view.EditorCanvas;
import texteditor.view.caret.CaretController;

public class EditorController {
    private final Scene scene;
    private final PieceTable document;
    private final CaretController caretController;
    private final EditorCanvas canvas;
    private final Caret cursor;

    public EditorController(Scene scene, PieceTable document, Caret cursor, CaretController caretController, EditorCanvas canvas) {
        this.scene = scene;
        this.document = document;
        this.caretController = caretController;
        this.cursor = cursor;
        this.canvas = canvas;



        setupKeyHandlers();


    }

    private void setupKeyHandlers() {
        scene.setOnKeyPressed(event -> {
            boolean modelChanged = false;

            if (event.getCode() == KeyCode.LEFT) {
                caretController.moveLeft();
                modelChanged = true;
            } else if (event.getCode() == KeyCode.RIGHT) {
                caretController.moveRight();
                modelChanged = true;
            } else if (event.getCode() == KeyCode.UP) {
                caretController.moveVertical(canvas.recalculateLayout(), -1);
                modelChanged = true;
            } else if (event.getCode() == KeyCode.DOWN) {
                caretController.moveVertical(canvas.recalculateLayout(), 1);
                modelChanged = true;
            } else if (event.getCode() == KeyCode.END) {
                caretController.moveToLineEnd(canvas.recalculateLayout());
                modelChanged = true;
            } else if (event.getCode() == KeyCode.HOME) {
                caretController.moveToLineStart(canvas.recalculateLayout());
                modelChanged = true;
            } else if (event.getCode() == KeyCode.ENTER) {
                document.insertText(cursor.getPosition(), "\n");
                caretController.moveRight();
                modelChanged = true;
            } else if (event.getCode().isLetterKey() || event.getCode().isDigitKey() || event.getText().length() == 1) {
                document.insertText(cursor.getPosition(), event.getText());
                caretController.moveRight();
                modelChanged = true;
            } else if (event.getCode() == KeyCode.BACK_SPACE) {
                document.removeText(cursor.getPosition() - 1, 1);
                caretController.moveLeft();
                modelChanged = true;
            }
            if (modelChanged) {
                canvas.resetCursorBlink();
                canvas.draw();
            }
        });
    }
}

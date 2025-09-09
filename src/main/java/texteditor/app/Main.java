package texteditor.app;

import javafx.application.Application;
import javafx.scene.text.Font;
import javafx.stage.Stage;
import javafx.scene.Scene;
import javafx.scene.layout.StackPane;
import texteditor.controller.EditorController;
import texteditor.model.Caret;
import texteditor.model.PieceTable;
import texteditor.view.EditorCanvas;
import texteditor.view.CanvasRenderer;
import texteditor.view.caret.CaretController;
import texteditor.view.layout.LayoutEngine;
import texteditor.view.text.JavaFXTextMeasurer;
import texteditor.view.text.TextMeasurer;

public class Main extends Application {
    private static final String INITIAL_TEXT =
            "Ends at 11\n" +
                    "This should start at 26 the length is harder to know because this sentence is longer";


    @Override
    public void start(Stage stage) {
            PieceTable document = new PieceTable(INITIAL_TEXT);
            Caret caret = new Caret(document);
            TextMeasurer textMeasurer = new JavaFXTextMeasurer(new Font("Consolas", 26));
            LayoutEngine layoutEngine = new LayoutEngine(textMeasurer);
            CaretController caretController = new CaretController(document, textMeasurer, caret, 10.0, 25.0);
            CanvasRenderer renderer = new CanvasRenderer(textMeasurer, 10.0, 25.0);

            EditorCanvas canvas = new EditorCanvas(document, layoutEngine, caretController, renderer, 10.0, 25.0);
            canvas.draw();

            StackPane root = new StackPane(canvas);
            Scene scene = new Scene(root, 300, 300);

            // hand off to controller
            new EditorController(scene, document, caret, caretController, canvas);

            stage.setTitle("Minimal Text Editor - M0");
            stage.setScene(scene);
            stage.show();
    }

}

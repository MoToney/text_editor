package texteditor.app;

import javafx.application.Application;
import javafx.scene.text.Font;
import javafx.stage.Stage;
import javafx.scene.Scene;
import javafx.scene.layout.StackPane;
import texteditor.controller.EditorController;
import texteditor.model.CursorModel;
import texteditor.model.PieceTable;
import texteditor.view.EditorCanvas;
import texteditor.view.cursor.CursorManager;
import texteditor.view.layout.TextLayoutEngine;
import texteditor.view.text.JavaFXTextMeasurer;
import texteditor.view.text.TextMeasurer;

public class Main extends Application {
    private static final String INITIAL_TEXT =
            "Ends at 11\n" +
                    "This should start at 26 the length is harder to know because this sentence is longer";


    @Override
    public void start(Stage stage) {
            PieceTable document = new PieceTable(INITIAL_TEXT);
            TextMeasurer textMeasurer = new JavaFXTextMeasurer(new Font("Consolas", 26));
            TextLayoutEngine layoutEngine = new TextLayoutEngine(textMeasurer);
            CursorManager cursorCalculator = new CursorManager(document);


            EditorCanvas canvas = new EditorCanvas(document, layoutEngine, cursorCalculator, 10.0, 25.0);
            CursorModel cursor = new CursorModel(document, canvas);

            canvas.setCursor(cursor);
            canvas.draw();

            StackPane root = new StackPane(canvas);
            Scene scene = new Scene(root, 300, 300);

            // hand off to controller
            new EditorController(scene, document, cursor, canvas);

            stage.setTitle("Minimal Text Editor - M0");
            stage.setScene(scene);
            stage.show();
    }
}

package texteditor.app;

import javafx.application.Application;
import javafx.scene.text.Text;
import javafx.stage.Stage;
import javafx.scene.Scene;
import javafx.scene.Parent;
import javafx.scene.layout.StackPane;
import javafx.scene.canvas.Canvas;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.layout.BorderPane;
import javafx.scene.paint.Color;
import texteditor.app.model.PieceTable;
import texteditor.app.render.Renderer;

public class Main extends Application {
    private static final String INITIAL_TEXT = "Hello, world! This is a simple text editor.";

    @Override
    public void start(Stage stage) {
        // Create the PieceTable with static text as per the M0 plan.
        PieceTable document = new PieceTable(INITIAL_TEXT);

        // Create the JavaFX window with a Canvas.
        Canvas canvas = new Canvas(800, 600);
        StackPane root = new StackPane(canvas);
        Scene scene = new Scene(root, 800, 600);
        Renderer renderer = new Renderer();

        // Configure the Stage
        stage.setTitle("Minimal Text Editor - M0");
        stage.setScene(scene);
        stage.show();

        // Get the GraphicsContext to draw on the Canvas.
        GraphicsContext gc = canvas.getGraphicsContext2D();

        // Draw the document content to the Canvas.
           renderer.drawDocument(gc, document.getText());
    }

    public static void main(String[] args) {
        launch(args);
    }
}

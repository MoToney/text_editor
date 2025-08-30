package texteditor.app;

import javafx.application.Application;
import javafx.scene.input.KeyEvent;
import javafx.scene.paint.Color;
import javafx.scene.text.Font;
import javafx.stage.Stage;
import javafx.scene.Scene;
import javafx.scene.layout.StackPane;
import javafx.scene.canvas.Canvas;
import javafx.scene.canvas.GraphicsContext;
import texteditor.model.PieceTable;
import texteditor.view.Renderer;

public class Main extends Application {
    private static final String INITIAL_TEXT = "Hello, world! This is a simple text editor.";

    @Override
    public void start(Stage stage) {
        // Create the PieceTable with static text as per the M0 plan.
        PieceTable document = new PieceTable(INITIAL_TEXT);
        Renderer renderer = new Renderer();


        // Create the JavaFX window with a Canvas.
        Canvas canvas = new Canvas(800, 600);
        GraphicsContext gc = canvas.getGraphicsContext2D();

        // Initial drawing of the scene
        renderer.drawDocument(gc, document.getText());


        StackPane root = new StackPane(canvas);
        Scene scene = new Scene(root, 800, 600);

        // Configure the Stage
        stage.setTitle("Minimal Text Editor - M0");
        stage.setScene(scene);
        stage.show();

        // Get the GraphicsContext to draw on the Canvas.

        // Draw the document content to the Canvas.
           renderer.drawDocument(gc, document.getText());
    }

    public static void main(String[] args) {
        launch(args);
    }}
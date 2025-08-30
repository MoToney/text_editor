package texteditor.view;

import javafx.scene.canvas.GraphicsContext;
import javafx.scene.paint.Color;
import javafx.scene.text.Font;

public class Renderer {
    private  double cursorX = 50.0;
    private  double cursorY = 50.0;

    public void drawDocument(GraphicsContext gc, String text) {
        // Set up the font and color for drawing.
        gc.clearRect(0, 0, gc.getCanvas().getWidth(), gc.getCanvas().getHeight());
        gc.setFont(new Font("Monospaced", 18));

        // Draw the text at a specific position.
        double x = 10;
        double y = 25;
        gc.fillText(text, x, y);
        drawCursor(gc);
    }

    public void drawCursor(GraphicsContext gc) {
        double cursorHeight = 18; // Should match font size
        gc.setStroke(Color.BLACK);
        gc.setLineWidth(1.5);
        // The y-coordinate is adjusted to align with text baseline
        gc.strokeLine(cursorX, cursorY - cursorHeight, cursorX, cursorY);
    }
}

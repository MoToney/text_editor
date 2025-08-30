package texteditor.app.render;

import javafx.scene.canvas.GraphicsContext;
import javafx.scene.text.Font;

public class Renderer {
    public void drawDocument(GraphicsContext gc, String text) {
        // Set up the font and color for drawing.
        gc.clearRect(0, 0, gc.getCanvas().getWidth(), gc.getCanvas().getHeight());
        gc.setFont(new Font("Monospaced", 18));

        // Draw the text at a specific position.
        double x = 10;
        double y = 25;
        gc.fillText(text, x, y);
    }
}

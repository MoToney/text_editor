package texteditor.view;

import javafx.scene.canvas.GraphicsContext;
import javafx.scene.paint.Color;
import texteditor.view.layout.VisualLine;
import texteditor.view.text.TextMeasurer;

import java.util.List;

public class EditorRenderer {
    private final TextMeasurer measurer;
    private final double paddingHorizontal;
    private final double paddingTop;

    public EditorRenderer(TextMeasurer measurer, double paddingHorizontal, double paddingTop) {
        this.measurer = measurer;
        this.paddingHorizontal = paddingHorizontal;
        this.paddingTop = paddingTop;
    }

    public void renderDocument(GraphicsContext gc, List<VisualLine> visualLines) {
        gc.setFont(measurer.getFont());

        for (int l = 0; l < visualLines.size(); l++) {
            String lineToDraw = visualLines.get(l).text();
            double y = paddingTop + measurer.getBaselineOffset() + (l * measurer.getLineHeight());
            gc.fillText(lineToDraw, paddingHorizontal, y);
        }
    }

    public void renderCursor(GraphicsContext gc, double cursorX, double cursorY, boolean visible) {
        if (!visible) {
            return;
        }
        // Calculate the top of the line by subtracting the baseline offset from the cursor's Y.
        double lineTop = cursorY - measurer.getBaselineOffset();

        // Calculate the bottom of the line.
        double lineBottom = lineTop + measurer.getLineHeight();

        gc.setStroke(Color.BLACK);
        gc.setLineWidth(1.5);
        gc.strokeLine(cursorX, lineTop, cursorX, lineBottom);
    }
}

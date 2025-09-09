package texteditor.view;

import javafx.scene.canvas.GraphicsContext;
import javafx.scene.paint.Color;
import texteditor.view.layout.VisualLine;
import texteditor.view.text.TextMeasurer;

import java.util.List;

public class CanvasRenderer {
    private final TextMeasurer measurer;
    private final double paddingHorizontal;
    private final double paddingTop;

    public CanvasRenderer(TextMeasurer measurer, double paddingHorizontal, double paddingTop) {
        this.measurer = measurer;
        this.paddingHorizontal = paddingHorizontal;
        this.paddingTop = paddingTop;
    }

    public void drawDocumentLines(GraphicsContext gc, List<VisualLine> visualLines) {
        if (gc == null) throw new IllegalArgumentException("gc is null");
        if (visualLines == null || visualLines.isEmpty()) return;

        var font = measurer.getFont();
        double baseline = measurer.getBaselineOffset();
        double lineHeight = measurer.getLineHeight();

        gc.setFont(font);
        gc.setFill(Color.BLACK);

        for (int l = 0; l < visualLines.size(); l++) {
            VisualLine visualLine = visualLines.get(l);
            String lineToDraw = visualLine.text();
            double y = paddingTop + baseline + (l * lineHeight);
            gc.fillText(lineToDraw, paddingHorizontal, y);
        }
    }

    public void drawCaret(GraphicsContext gc, double cursorX, double cursorY, boolean visible) {
        if (gc == null) throw new IllegalArgumentException("gc is null");
        if (!visible) {return;}

        double lineTop = cursorY - measurer.getBaselineOffset();
        double lineHeight = measurer.getLineHeight();

        gc.setLineWidth(1.5);
        gc.setStroke(Color.BLACK);

        double x = Math.round(cursorX) + 0.5;
        double top = Math.round(lineTop) + 0.5;
        double bottom = Math.round(lineTop + lineHeight) + 0.5;
        gc.strokeLine(x, top, x, bottom);
    }
}

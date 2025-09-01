package texteditor.view;

import javafx.animation.KeyFrame;
import javafx.animation.Timeline;
import javafx.geometry.Bounds;
import javafx.scene.canvas.Canvas;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.paint.Color;
import javafx.scene.text.Font;
import javafx.scene.text.Text;
import javafx.util.Duration;
import texteditor.model.CursorModel;
import texteditor.model.PieceTable;

public class EditorCanvas extends Canvas {
    private final PieceTable document;
    private final CursorModel cursor;

    private final Font font = new Font("Monospaced", 26);
    private double paddingLeft = 10.0;
    private double paddingTop = 25.0;
    private double lineHeight = 18.0;
    private double baselineOffset = 14.0;

    private  double cursorX = 0;
    private  double cursorY = 0;

    private boolean isCursorVisible = true;
    private final Timeline cursorBlinkTimeline;

    public EditorCanvas(PieceTable document, CursorModel cursor) {
        super(800,600);
        this.document = document;
        this.cursor = cursor;

        this.cursorBlinkTimeline = new Timeline(
            new KeyFrame(Duration.seconds(0.5), event -> {
                isCursorVisible = !isCursorVisible;
                draw();
            })
        );
        cursorBlinkTimeline.setCycleCount(Timeline.INDEFINITE);
        cursorBlinkTimeline.play();

        this.focusedProperty().addListener((observable, oldValue, newValue) -> {
            if (newValue) {
                resetCursorBlink();
                cursorBlinkTimeline.play();
            } else {
                isCursorVisible = false;
                draw();
                cursorBlinkTimeline.pause();
            }
        });
    }

    public void resetCursorBlink() {
        isCursorVisible = true;
        cursorBlinkTimeline.playFromStart();
    }

    public void calculateFontMetrics() {
        Text textMetrics = new Text("T");
        textMetrics.setFont(font);

        double calculatedLineSpacing = textMetrics.getLineSpacing();

        // The fallback is kept for robustness, but should no longer be triggered.
        if (calculatedLineSpacing <= 0) {
            Bounds bounds = textMetrics.getLayoutBounds();
            this.lineHeight = bounds.getHeight() * 1.2;
        } else {
            this.lineHeight = calculatedLineSpacing;
        }

        this.baselineOffset = textMetrics.getBaselineOffset();
    }

    public void draw() {
        GraphicsContext gc = this.getGraphicsContext2D();
        gc.clearRect(0, 0, getWidth(), getHeight());
        gc.setFont(font);

        drawDocument(gc);
        updateCursorLocation();
        drawCursor(gc);
    }

    public void drawDocument(GraphicsContext gc) {
        // In EditorCanvas.drawDocumentText()
        int lineCount = document.getLineCount();
        for (int i = 0; i < lineCount; i++) {
            String lineText = document.getLine(i);
            double y = paddingTop + baselineOffset + (i * lineHeight);
            gc.fillText(lineText, paddingLeft, y);
        }
    }

    public void updateCursorLocation() {
        int cursorPosition = cursor.getPosition();

        int lineIndex = document.getLineIndex(cursorPosition);
        int columnIndex = document.getColumnIndex(cursorPosition);

        String lineText = document.getLine(lineIndex);
        String textBeforeCursor = lineText.substring(0, Math.min(columnIndex, lineText.length()));

        Text textToMeasure = new Text(textBeforeCursor);
        textToMeasure.setFont(font);
        double textWidth = textToMeasure.getLayoutBounds().getWidth();

        this.cursorX = paddingLeft + textWidth;
        this.cursorY = paddingTop + baselineOffset + (lineIndex * lineHeight);
    }

    public void drawCursor(GraphicsContext gc) {
        if (!isCursorVisible) {
            return;
        }
        double cursorHeight = 20;
        gc.setStroke(Color.BLACK);
        gc.setLineWidth(1.5);
        gc.strokeLine(cursorX, cursorY - cursorHeight, cursorX, cursorY);
    }




}

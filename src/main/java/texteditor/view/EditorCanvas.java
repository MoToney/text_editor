package texteditor.view;

import javafx.animation.KeyFrame;
import javafx.animation.Timeline;
import javafx.scene.canvas.Canvas;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.paint.Color;
import javafx.scene.text.Font;
import javafx.util.Duration;
import texteditor.model.CursorModel;
import texteditor.model.PieceTable;
import texteditor.view.cursor.CursorManager;
import texteditor.view.layout.TextLayoutEngine;
import texteditor.view.layout.VisualLine;
import texteditor.view.text.JavaFXTextMeasurer;
import texteditor.view.text.TextMeasurer;

import java.util.ArrayList;
import java.util.List;

public class EditorCanvas extends Canvas {
    private final PieceTable document;
    private final TextLayoutEngine layoutEngine;
    private final CursorManager cursorCalculator;

    private CursorModel cursor;
    private  List<VisualLine> visualLines = new ArrayList<>();

    private final Font font = new Font("Consolas", 26);
    private final TextMeasurer measurer = new JavaFXTextMeasurer(font);

    private final double paddingHorizontal;
    private final double paddingTop;

    private double cursorX = 0;
    private double cursorY = 0;

    private boolean isCursorVisible = true;
    private final Timeline cursorBlinkTimeline;

    public EditorCanvas(PieceTable document, TextLayoutEngine layoutEngine, CursorManager cursorCalculator,
                        double paddingHorizontal, double paddingTop) {
        super(250, 300);

        this.document = document;
        this.layoutEngine = layoutEngine;
        this.cursorCalculator = cursorCalculator;
        this.cursor = null;

        this.paddingHorizontal = paddingHorizontal;
        this.paddingTop = paddingTop;

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

    public void setCursor(CursorModel cursor) {
        this.cursor = cursor;
    }

    public void resetCursorBlink() {
        isCursorVisible = true;
        cursorBlinkTimeline.playFromStart();
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
        double availableWidth = getWidth() - paddingHorizontal - paddingHorizontal;
        var layoutResult = layoutEngine.calculateLayout(document, availableWidth);
        visualLines = layoutResult.getVisualLines();

        // Draw visual lines
        for (int l = 0; l < visualLines.size(); l++) {
            String lineToDraw = visualLines.get(l).text();
            double y = paddingTop + measurer.getBaselineOffset() + (l * measurer.getLineHeight());
            gc.fillText(lineToDraw, paddingHorizontal, y);
        }
    }

    public void moveLeft() {
        if (cursor == null) return;
        var newPosition = cursorCalculator.calculateLeftMovement(cursor);
        cursor.setPosition(newPosition.position());
        cursor.setAffinity(newPosition.affinity());
        resetCursorBlink();
        draw();
    }

    public void moveRight() {
        if (cursor == null) return;
        var newPosition = cursorCalculator.calculateRightMovement(cursor);
        cursor.setPosition(newPosition.position());
        cursor.setAffinity(newPosition.affinity());
        resetCursorBlink();
        draw();
    }

    public void moveEnd() {
        if (cursor == null) return;
        var newPosition = cursorCalculator.calculateLineEndMovement(cursor, visualLines);
        cursor.setPosition(newPosition.position());
        cursor.setAffinity(newPosition.affinity());
        resetCursorBlink();
        draw();
        }

    public void moveHome() {
        if (cursor == null) return;
        var newPosition = cursorCalculator.calculateLineStartMovement(cursor, visualLines);
        cursor.setPosition(newPosition.position());
        cursor.setAffinity(newPosition.affinity());
        resetCursorBlink();
        draw();
    }

    public void moveDown() {
        if (cursor == null) return;
        var newPosition = cursorCalculator.calculateVerticalMovement(cursor, visualLines, 1);
        cursor.setPosition(newPosition.position());
        cursor.setAffinity(newPosition.affinity());
        resetCursorBlink();
        draw();
    }

    public void moveUp() {
        if (cursor == null) return;
        var newPosition = cursorCalculator.calculateVerticalMovement(cursor, visualLines, -1);
        cursor.setPosition(newPosition.position());
        cursor.setAffinity(newPosition.affinity());
        resetCursorBlink();
        draw();
    }

    public void updateCursorLocation() {
        if (cursor == null ) return;

        int pos = cursor.getPosition();
        int vIndex =cursorCalculator.findVisualLineIndex(pos, cursor.getAffinity(), visualLines);
        vIndex = cursorCalculator.adjustForAffinity(pos, vIndex, cursor.getAffinity(), visualLines);

        if (vIndex < 0) {
            vIndex = visualLines.size() - 1;
        }

        VisualLine vline = visualLines.get(vIndex);
        int lineStart = vline.startPosition();
        int col = pos - lineStart;
        col = Math.max(0, Math.min(col, vline.length()));

        double x;
        if (col == 0) {
            x = paddingHorizontal;
        } else {
            // safe: col is clamped into [0, len]
            String before = vline.text().substring(0, col);
            x = paddingHorizontal + measurer.measureWidth(before);
        }

        double y = paddingTop + measurer.getBaselineOffset() + (vIndex * measurer.getLineHeight());

        this.cursorX = x;
        this.cursorY = y;
    }

    public void drawCursor(GraphicsContext gc) {
        if (!isCursorVisible) {
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


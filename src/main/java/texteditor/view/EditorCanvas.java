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
import texteditor.view.cursor.CursorPositionCalculator;
import texteditor.view.layout.TextLayoutEngine;
import texteditor.view.layout.VisualLine;
import texteditor.view.text.JavaFXTextMeasurer;
import texteditor.view.text.TextMeasurer;

import java.util.ArrayList;
import java.util.List;

public class EditorCanvas extends Canvas {
    private final PieceTable document;
    private final TextLayoutEngine layoutEngine;
    private final CursorPositionCalculator cursorCalculator;

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

    public EditorCanvas(PieceTable document, TextLayoutEngine layoutEngine, CursorPositionCalculator cursorCalculator,
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
        int currentPos = cursor.getPosition();
        if (currentPos > document.getDocumentLength()) { return; }

        int index = findVisualLineIndexForPosition(currentPos);
        if (index == -1 || index == visualLines.size() - 1)  return;

        VisualLine cur = visualLines.get(index);
        int start = cur.startPosition();
        int col = findVisualColumnByLine(cur);

        VisualLine nextLine = visualLines.get(index + 1);
        if (col >= nextLine.length()) {
            if (nextLine.hasNewlineChar()) {
                cursor.setPosition(nextLine.length() - 1 + nextLine.startPosition());
            } else {
                cursor.setPosition(nextLine.length() + nextLine.startPosition());
                cursor.setAffinity(CursorModel.Affinity.LEFT);
            }
        } else {
            cursor.setPosition(nextLine.startPosition() + col);
            }

        resetCursorBlink();
        draw();
    }

    public void moveUp() {
        int currentPos = cursor.getPosition();
        if (currentPos > document.getDocumentLength()) { return; }

        int index = findVisualLineIndexForPosition(currentPos);
        if (index < 1)  return;

        VisualLine cur = visualLines.get(index);
        int start = cur.startPosition();
        int col = findVisualColumnByLine(cur);

        VisualLine prevLine = visualLines.get(index - 1);
        if (col >= prevLine.length()) {
            if (prevLine.hasNewlineChar()) {
                cursor.setPosition(prevLine.length() - 1 + prevLine.startPosition());
            } else {
                cursor.setPosition(prevLine.length() + prevLine.startPosition());
                cursor.setAffinity(CursorModel.Affinity.LEFT);
            }
        } else {
            cursor.setPosition(prevLine.startPosition() + col);
        }
        resetCursorBlink();
        draw();

    }

    public int findVisualColumnByLine(VisualLine line) {
        return cursor.getPosition() - line.startPosition();
    }


    public void updateCursorLocation() {
        if (cursor == null ) return;

        int pos = cursor.getPosition();
        int vIndex =findVisualLineIndexForPosition(pos);

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

    public int findVisualLineIndexForPosition(int position) {
            if (visualLines.isEmpty()) return -1;

            int docLen = document.getDocumentLength();
            position = Math.max(0, Math.min(position, docLen));

            for (int i = 0; i < visualLines.size(); i++) {
                VisualLine cur = visualLines.get(i);
                int start = cur.startPosition();
                int endEx = start + cur.length();

                // if position is within this line
                if (position > start && position < endEx) {
                    return i;
                }

                // if position is at the start of this line
                if (position == start) {
                    if (i == 0) return 0; // Very first character must be on the first line.
                    // Affinity decides: RIGHT belongs to the previous line.
                    return (cursor.getAffinity() == CursorModel.Affinity.RIGHT) ? i - 1 : i;
                }

                // Case 3: Position is at the end of this line.
                if (position == endEx) {
                    if (i == visualLines.size() - 1) return i; // Very last position must be on the last line.
                    // For a soft wrap, affinity decides: LEFT belongs to this line.
                    return (cursor.getAffinity() == CursorModel.Affinity.LEFT) ? i : i + 1;
                }
            }

            if (position >= docLen) return visualLines.size() - 1;

            return Math.max(0, Math.min(visualLines.size() - 1, position));
        }

    public int getVisualLineIndex(int position) {
        if (visualLines.isEmpty()) return -1;

        int docLen = document.getDocumentLength();
        if (position < 0) return 0;

        for (int i = 0; i < visualLines.size(); i++) {
            VisualLine cur = visualLines.get(i);
            int start = cur.startPosition();                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                        
            int endEx = start + cur.length();

            // if position is within this line (including the end boundary)
            if (position >= start && position < endEx) {
                return i;
            }
        }

        if (position >= docLen) return visualLines.size() - 1;

        return Math.max(0, Math.min(visualLines.size() - 1, position));
    }



    public int getVisualLineEndPosition(int index) {
        return this.visualLines.get(index).startPosition() + this.visualLines.get(index).length();
    }

    public int findVisualLineEnd(int pos) {
        int index = getVisualLineIndex(pos);
        if (index < 0) return -1;
        VisualLine cur = visualLines.get(index);
        int start = cur.startPosition();
        int endEx = start + cur.length(); // exclusive end
        if (cur.hasNewlineChar()) {
            return 0;
        }
        return 0;
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


    public List<VisualLine> getVisualLines() {
        return this.visualLines;
    }

    public VisualLine getVisualLine(int index) {
        return this.visualLines.get(index);
    }

    public String getVisualLineText(int index) {
        return this.visualLines.get(index).text();
    }

    public int getVisualLineStartPosition(int index) {
        return this.visualLines.get(index).startPosition();
    }

    public int getVisualLineLength(int index) {
        return this.visualLines.get(index).length();
    }

    public int getVisualLineColumn(int visualLineIndex, int position) {
        VisualLine line = visualLines.get(visualLineIndex);
        int column = position - line.startPosition();
        return column;
    }

    public int getVisualPosition(int visualLineIndex, int column) {
        VisualLine line = visualLines.get(visualLineIndex);
        int i = line.startPosition() + column;
        return i;
    }

    public boolean isLastLine(int visualLineIndex) {
        return visualLineIndex == visualLines.size() - 1;
    }
}


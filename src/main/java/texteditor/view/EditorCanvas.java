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

import java.util.ArrayList;
import java.util.List;

public class EditorCanvas extends Canvas {
    private final PieceTable document;
    private CursorModel cursor;
    private final List<VisualLine> visualLines = new ArrayList<>();



    private final Font font = new Font("Monospaced", 26);
    private final Text textMetrics = new Text();
    private double paddingLeft = 10.0;
    private double paddingRight = 10.0;
    private double paddingTop = 25.0;
    private double lineHeight = 18.0;
    private double baselineOffset = 14.0;

    private double cursorX = 0;
    private double cursorY = 0;

    private boolean isCursorVisible = true;
    private final Timeline cursorBlinkTimeline;

    private boolean phantomAtEnd = false;
    private int phantomLineIndex = -1;
    private final double phantomOverhangPx = 2.0;

    public EditorCanvas(PieceTable document) {
        super(250, 300);
        this.document = document;
        this.cursor = null;

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

    public record VisualLine(String text, int startPosition, boolean hasNewLineChar) {
        public int length() {
            return text.length();
        }

        public boolean endsWithNewLineChar() {
            return hasNewLineChar;
        }
    }

    public void setCursor(CursorModel cursor) {
        this.cursor = cursor;
    }

    public void resetCursorBlink() {
        isCursorVisible = true;
        cursorBlinkTimeline.playFromStart();
    }

    public void calculateFontMetrics() {
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

    private double measureWidth(String s) {
        if (s == null || s.isEmpty()) return 0.0;
        textMetrics.setText(s);
        return textMetrics.getLayoutBounds().getWidth();
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
        this.visualLines.clear();
        double availableWidth = getWidth() - paddingLeft - paddingRight;
        int logicalLineStartPosition = 0;
        int lineCount = document.getLineCount();

        for (int i = 0; i < lineCount; i++) {
            String logicalLine = document.getLine(i);

            // --- Extract any trailing newline sequence and keep the text content separate ---
            String newline = "";
            String content = logicalLine;
            if (content.endsWith("\r\n")) {
                newline = "\r\n";
                content = content.substring(0, content.length() - 2);
            } else if (content.endsWith("\n") || content.endsWith("\r")) {
                newline = content.substring(content.length() - 1);
                content = content.substring(0, content.length() - 1);
            }

            // Measure the full visible width using content only (newline has no horizontal width)
            textMetrics.setText(content);
            double contentWidth = textMetrics.getLayoutBounds().getWidth();

            int logicalLineLength = document.getLineLength(i);
            boolean hasHardLineBreak = !newline.isEmpty(); // whether this logical line ended with newline

            // If entire content fits, create a single visual line containing content + newline (if any)
            if (contentWidth <= availableWidth) {
                String textForVisual = content + newline; // newline preserved for caret/indexing
                visualLines.add(new VisualLine(textForVisual, logicalLineStartPosition, hasHardLineBreak));
            } else {
                // Wrapping: operate on content (no newline). Ensure the newline is appended to the last piece.
                String remaining = content;
                int visualStartPos = logicalLineStartPosition;

                while (!remaining.isEmpty()) {
                    int breakPoint = -1;
                    // Find largest j such that remaining.substring(0, j) fits
                    for (int j = 1; j <= remaining.length(); j++) {
                        String sub = remaining.substring(0, j);
                        if (measureWidth(sub) > availableWidth) {
                            breakPoint = j - 1;
                            break;
                        }
                    }
                    if (breakPoint <= 0) breakPoint = remaining.length();

                    String piece = remaining.substring(0, breakPoint);
                    remaining = remaining.substring(breakPoint);

                    // If this is the last piece (no remaining content) and there is a newline, append it
                    boolean isLastPiece = remaining.isEmpty();
                    String pieceToStore = isLastPiece && !newline.isEmpty() ? piece + newline : piece;
                    boolean pieceHasHardBreak = isLastPiece && hasHardLineBreak;

                    visualLines.add(new VisualLine(pieceToStore, visualStartPos, pieceHasHardBreak));

                    // Advance the visualStartPos by the logical characters we just assigned.
                    // Note: newline contributes to logical positions only when appended to the last piece.
                    visualStartPos += piece.length();
                    if (isLastPiece && !newline.isEmpty()) {
                        visualStartPos += newline.length(); // account for \n or \r\n in the buffer index
                    }
                }
            }

            // Advance the logicalLineStartPosition by the full logical length (includes newline if present)
            logicalLineStartPosition += logicalLineLength;
        }

        // Draw visual lines
        for (int l = 0; l < visualLines.size(); l++) {
            String lineToDraw = visualLines.get(l).text;
            double y = paddingTop + baselineOffset + (l * lineHeight);
            gc.fillText(lineToDraw, paddingLeft, y);
        }
    }

    public void moveLeft() {
        if (cursor == null) return;
        int currentPos = cursor.getPosition();
        if (currentPos > 0) {
            cursor.setPosition(currentPos - 1);
            // Set RIGHT affinity for backward navigation.
            cursor.setAffinity(CursorModel.Affinity.RIGHT);
        }
        resetCursorBlink();
        draw();
    }

    public void moveRight() {
        if (cursor == null) return;
        int currentPos = cursor.getPosition();
        if (currentPos < document.getDocumentLength()) {
            cursor.setPosition(currentPos + 1);
            // Set LEFT affinity for forward navigation.
            cursor.setAffinity(CursorModel.Affinity.RIGHT);
        }
        resetCursorBlink();
        draw();
    }

    public void moveEnd() {
        if (cursor == null) return;
        int currentPos = cursor.getPosition();
        if (currentPos > document.getDocumentLength()) { return; }

        int index = findVisualLineIndexForPosition(currentPos);
        if (index == -1)  return;

        VisualLine cur = visualLines.get(index);
        int start = cur.startPosition;
        int endEx = start + cur.length();

        if (cur.hasNewLineChar) {
           cursor.setPosition(endEx - 1);
        } else {
            cursor.setPosition(endEx);
            cursor.setAffinity(CursorModel.Affinity.LEFT);
        }

        resetCursorBlink();
        draw();
        }


    public void updateCursorLocation() {
        if (cursor == null || visualLines.isEmpty()) return;

        int pos = cursor.getPosition();
        int vIndex =findVisualLineIndexForPosition(pos);

        if (vIndex < 0) {
            vIndex = visualLines.size() - 1;
        }

        VisualLine vline = visualLines.get(vIndex);
        int lineStart = vline.startPosition;
        int col = pos - lineStart;
        col = Math.max(0, Math.min(col, vline.length()));

        double x;
        if (col == 0) {
            x = paddingLeft;
        } else {
            // safe: col is clamped into [0, len]
            String before = vline.text().substring(0, col);
            x = paddingLeft + measureWidth(before);
        }

        double y = paddingTop + baselineOffset + (vIndex * lineHeight);

        this.cursorX = x;
        this.cursorY = y;
    }

    public int findVisualLineIndexForPosition(int position) {
            if (visualLines.isEmpty()) return -1;

            int docLen = document.getDocumentLength();
            position = Math.max(0, Math.min(position, docLen));

            for (int i = 0; i < visualLines.size(); i++) {
                VisualLine cur = visualLines.get(i);
                int start = cur.startPosition;
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
            int start = cur.startPosition;                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                        
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
        return this.visualLines.get(index).startPosition + this.visualLines.get(index).length();
    }

    public int findVisualLineEnd(int pos) {
        int index = getVisualLineIndex(pos);
        if (index < 0) return -1;
        VisualLine cur = visualLines.get(index);
        int start = cur.startPosition;
        int endEx = start + cur.length(); // exclusive end
        if (cur.hasNewLineChar()) {
            return 0;
        }
        return 0;
    }


    public void drawCursor(GraphicsContext gc) {
        if (!isCursorVisible) {
            return;
        }
        // Calculate the top of the line by subtracting the baseline offset from the cursor's Y.
        double lineTop = cursorY - baselineOffset;

        // Calculate the bottom of the line.
        double lineBottom = lineTop + lineHeight;

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
        return this.visualLines.get(index).text;
    }

    public int getVisualLineStartPosition(int index) {
        return this.visualLines.get(index).startPosition;
    }

    public int getVisualLineLength(int index) {
        return this.visualLines.get(index).length();
    }

    public int getVisualLineColumn(int visualLineIndex, int position) {
        VisualLine line = visualLines.get(visualLineIndex);
        int column = position - line.startPosition;
        return column;
    }

    public int getVisualPosition(int visualLineIndex, int column) {
        VisualLine line = visualLines.get(visualLineIndex);
        return line.startPosition + column;
    }

    public boolean isLastLine(int visualLineIndex) {
        return visualLineIndex == visualLines.size() - 1;
    }
}


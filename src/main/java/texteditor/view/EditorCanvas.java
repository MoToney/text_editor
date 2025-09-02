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
    public enum Affinity { LEFT, RIGHT }


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

    private Affinity caretAffinity = Affinity.RIGHT;
    private boolean phantomAtEnd = false;
    private int phantomLineIndex = -1;
    private final double phantomOverhangPx = 2.0;

    public EditorCanvas(PieceTable document) {
        super(800, 600);
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

    public record VisualLine(String text, int startPosition) {
        public int length() {
            return text.length();
        }
    }

    public void setCaretAffinity(Affinity a) { this.caretAffinity = a; }

    public void setPhantomAtEnd(int visualLineIndex) {
        this.phantomAtEnd = true;
        this.phantomLineIndex = visualLineIndex;
        this.caretAffinity = Affinity.RIGHT;
    }

    public void clearPhantom() {
        this.phantomAtEnd = false;
        this.phantomLineIndex = -1;
    }

    private int findVisualLineIndexWithAffinity(int position) {
        if (visualLines.isEmpty()) return -1;

        for (int i = 0; i < visualLines.size(); i++) {
            VisualLine cur = visualLines.get(i);
            int start = cur.startPosition;
            int endEx = start + cur.length(); // exclusive end

            // Normal interior membership
            if (position > start && position < endEx) return i;

            // Exactly at the start of this visual line
            if (position == start) {
                // If it's also the end of previous, choose based on affinity
                if (i > 0) {
                    VisualLine prev = visualLines.get(i - 1);
                    int prevEnd = prev.startPosition + prev.length();
                    if (prevEnd == start) {
                        return (caretAffinity == Affinity.RIGHT) ? i : (i - 1);
                    }
                }
                return i;
            }

            // Exactly at the end of this visual line
            if (position == endEx) {
                // If it's also the start of next, choose based on affinity
                if (i + 1 < visualLines.size()) {
                    VisualLine next = visualLines.get(i + 1);
                    if (next.startPosition == endEx) {
                        return (caretAffinity == Affinity.RIGHT) ? (i + 1) : i;
                    }
                }
                return i;
            }
        }

        // If position is beyond all lines and equals document length (EOF), return last line.
        int docLen = document.getDocumentLength();
        if (position >= docLen) return visualLines.size() - 1;

        return -1;
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
            textMetrics.setText(logicalLine);
            double lineWidth = textMetrics.getLayoutBounds().getWidth();

            int logicalLineLength = document.getLineLength(i);
            boolean hasTrailingNewLine = (i < lineCount - 1);

            if (lineWidth <= availableWidth) {
                visualLines.add(new VisualLine(logicalLine, logicalLineStartPosition));
            } else {
                String remainingText = logicalLine;
                int visualStartPos = logicalLineStartPosition;

                while (!remainingText.isEmpty()) {
                    int breakPoint = -1;
                    for (int j = 1; j <= remainingText.length(); j++) {
                        String sub = remainingText.substring(0, j);
                        textMetrics.setText(sub);
                        double subWidth = textMetrics.getLayoutBounds().getWidth();
                        if (subWidth > availableWidth) {
                            breakPoint = j - 1;
                            break;
                        }
                    }

                    if (breakPoint <= 0) breakPoint = remainingText.length();

                    String textThatFits = remainingText.substring(0, breakPoint);

                    visualLines.add(new VisualLine(textThatFits, visualStartPos));

                    visualStartPos += textThatFits.length();
                    remainingText = remainingText.substring(breakPoint);
                }
            }

            logicalLineStartPosition += logicalLineLength + (hasTrailingNewLine ? 1 : 0);
        }
            for (int l = 0; l < visualLines.size(); l++) {
                String lineToDraw = visualLines.get(l).text;
                double y = paddingTop + baselineOffset + (l * lineHeight);
                gc.fillText(lineToDraw, paddingLeft, y);
            }
        }


    public void updateCursorLocation() {
        if (cursor == null || visualLines.isEmpty()) return;

        int pos = cursor.getPosition();                // logical index in document
        int docLen = document.getDocumentLength();

        // Find visual line index respecting affinity
        int vIndex = findVisualLineIndexWithAffinity(pos);
        if (vIndex < 0) {
            // fallback: place at end of last line
            vIndex = visualLines.size() - 1;
        }

        VisualLine vline = visualLines.get(vIndex);
        int start = vline.startPosition;
        int len = vline.length();

        // Column is clamped between 0 and len (len means "after last char")
        int col = pos - start;
        if (col < 0) col = 0;
        if (col > len) col = len;

        // X coordinate
        double x;
        boolean drawingPhantomHere = (phantomAtEnd && vIndex == phantomLineIndex && col == len);
        if (drawingPhantomHere) {
            // place caret just past end-of-line for End key visual
            double textW = measureWidth(vline.text());
            x = paddingLeft + textW + phantomOverhangPx;
        } else {
            // measure width of substring (0..col)
            if (col == 0) {
                x = paddingLeft;
            } else {
                String before = vline.text().substring(0, col);
                x = paddingLeft + measureWidth(before);
            }
        }

        // Y coordinate
        double y = paddingTop + baselineOffset + (vIndex * lineHeight);

        // Assign
        this.cursorX = x;
        this.cursorY = y;
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

    public int getVisualLineEndPosition(int index) {
        return this.visualLines.get(index).startPosition + this.visualLines.get(index).length();
    }

    public int getVisualLineIndex(int position) {
        List<VisualLine> lines = this.visualLines;
        int currentVisualLineIndex;
        if (lines.isEmpty()) return -1;

        for (int i = 0; i < lines.size(); i++) {
            VisualLine line = lines.get(i);
            int lineEndPosition = line.startPosition + line.length();
            if (position >= line.startPosition && position <= lineEndPosition) {
                currentVisualLineIndex = i;
                return currentVisualLineIndex;
            }
        }
        return -1;
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


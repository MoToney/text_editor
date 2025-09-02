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
        int cursorPosition = cursor.getPosition();

        // 1. Loop through the VISUAL lines to find the cursor.
        for (int i = 0; i < visualLines.size(); i++) {
            VisualLine line = visualLines.get(i);
            int lineEndPosition = line.startPosition() + line.text().length();

            // Check if the cursor's absolute position is within this visual line's range.
            if (cursorPosition >= line.startPosition() && cursorPosition <= lineEndPosition) {

                // 2. Calculate the column relative to this VISUAL line.
                int visualColumn = cursorPosition - line.startPosition();

                // 3. Get the substring of the VISUAL line before the cursor.
                String textBeforeCursor = line.text().substring(0, visualColumn);

                // 4. Measure the substring to get the X coordinate.
                textMetrics.setText(textBeforeCursor);
                double textWidth = textMetrics.getLayoutBounds().getWidth();
                this.cursorX = paddingLeft + textWidth;

                // 5. Use the VISUAL line's index (i) to get the Y coordinate.
                this.cursorY = paddingTop + baselineOffset + (i * lineHeight);

                // We found the cursor's location, so we can exit the loop.
                return;
            }
        }
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


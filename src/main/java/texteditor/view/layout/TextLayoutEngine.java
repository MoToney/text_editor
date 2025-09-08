package texteditor.view.layout;

import texteditor.model.PieceTable;
import texteditor.view.text.TextMeasurer;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class TextLayoutEngine {

    private final TextMeasurer textMeasurer;

    public TextLayoutEngine(TextMeasurer textMeasurer) {
        this.textMeasurer = textMeasurer;
    }

    public LayoutResult calculateLayout(PieceTable document, double availableWidth) {
        if (document == null) {
            return new LayoutResult(Collections.emptyList());
        }

        List<VisualLine> visualLines = new ArrayList<>();
        int logicalLineStartPosition = 0;
        int lineCount = document.getLineCount();

        for (int i = 0; i < lineCount; i++) {
            String logicalLine = document.getLine(i);
            LineComponents components = parseLineComponents(logicalLine);

            double contentWidth = textMeasurer.measureWidth(components.content());

            if (contentWidth <= availableWidth) {
                createSingleVisualLine(visualLines, components, logicalLineStartPosition);
            } else {
                createWrappedVisualLines(visualLines, components, logicalLineStartPosition, availableWidth);
            }
            logicalLineStartPosition = document.getLineLength(i);
        }
        return new LayoutResult(visualLines);
    }

    public void createSingleVisualLine(List<VisualLine> visualLines, LineComponents components, int startPosition) {
        String fullText = components.content() + components.newline();
        boolean hasNewlineChar = !components.newline().isEmpty();
        visualLines.add(new VisualLine(fullText, startPosition, hasNewlineChar));
    }

    public void createWrappedVisualLines(List<VisualLine> visualLines, LineComponents components,
                                         int startPosition, double availableWidth) {
        String remainingString = components.content();
        int currentPosition = startPosition;

        while (!remainingString.isEmpty()) {
            int breakpoint = findOptimalBreakpoint(remainingString, availableWidth);
            String piece = remainingString.substring(0, breakpoint);
            remainingString = remainingString.substring(breakpoint);

            boolean isLastPiece = remainingString.isEmpty();
            String pieceText = isLastPiece ? piece + components.newline() : piece;
            boolean hasNewLineChar = isLastPiece && !components.newline().isEmpty();

            visualLines.add(new VisualLine(pieceText, currentPosition, hasNewLineChar));

            currentPosition += piece.length();
            if (isLastPiece && !components.newline().isEmpty()) {
                currentPosition += components.newline().length();
            }
        }
    }

    public int findOptimalBreakpoint(String text, double availableWidth) {
        int left = 0, right = text.length();
        while (left < right) {
            int mid = left + (right - left + 1) / 2; // +1 gets the last passing index and prevents infinite loop (upper bound)
            String sub = text.substring(0, mid);

            if (textMeasurer.measureWidth(sub) <= availableWidth) {
                left = mid;
            } else {
                right = mid - 1;
            }
        }
        return left;
    }

    public LineComponents parseLineComponents(String logicalLine) {
        if (logicalLine.endsWith("\n")) {
            return new LineComponents(logicalLine.substring(0, logicalLine.length() - 1), "\n");
        } else {
            return new LineComponents(logicalLine, "");
        }
    }


    private record LineComponents(String content, String newline) {
    }

    /**
     * Result object that can be extended with metadata like performance stats
     */

    public static class LayoutResult {
        private final List<VisualLine> visualLines;

        public LayoutResult(List<VisualLine> visualLines) {
            this.visualLines = Collections.unmodifiableList(new ArrayList<>(visualLines));
        }

        public List<VisualLine> getVisualLines() {
            return visualLines;
        }

        public int getLineCount() {
            return visualLines.size();
        }

        public boolean isEmpty() {
            return visualLines.isEmpty();
        }
    }
}

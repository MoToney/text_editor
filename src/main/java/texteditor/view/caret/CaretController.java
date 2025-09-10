package texteditor.view.caret;

import texteditor.model.Caret;
import texteditor.model.PieceTable;
import texteditor.view.layout.VisualLine;
import texteditor.view.text.TextMeasurer;

import java.util.List;

public class CaretController {
    private final PieceTable document;
    private final TextMeasurer measurer;
    private final Caret cursor;
    private double cursorX = 0;
    private double cursorY = 0;

    private final double paddingHorizontal;
    private final double paddingTop;

    public CaretController(PieceTable document, TextMeasurer measurer, Caret cursor, double paddingHorizontal, double paddingTop) {
        this.document = document;
        this.measurer = measurer;
        this.cursor = cursor;
        this.paddingHorizontal = paddingHorizontal;
        this.paddingTop = paddingTop;
    }

    public void moveLeft() {
        if (cursor == null) {return;}
        int currentPosition = cursor.getPosition();
        if (currentPosition > 0) {
            cursor.setPosition(currentPosition - 1);
            cursor.setAffinity(Caret.Affinity.RIGHT);
        }
    }

    public void moveRight() {
        if (cursor == null) {return;}
        int currentPosition = cursor.getPosition();
        if (currentPosition < document.getDocumentLength()) {
            cursor.setPosition(currentPosition + 1);
            cursor.setAffinity(Caret.Affinity.RIGHT);
        }
    }

    public void moveToLineStart(List<VisualLine> visualLines) {
        if (cursor == null) {return;}
        int currentPosition = cursor.getPosition();
        if (currentPosition > document.getDocumentLength()) {return;}

        int lineIndex = findVisualLineIndex(currentPosition, visualLines);
        lineIndex = adjustForAffinity(currentPosition, lineIndex, visualLines);
        if (lineIndex == -1) return;

        VisualLine line = visualLines.get(lineIndex);
        cursor.setPosition(line.startPosition());
        cursor.setAffinity(Caret.Affinity.RIGHT);
    }

    public void moveToLineEnd(List<VisualLine> visualLines) {
        if (cursor == null) {return;}
        int currentPosition = cursor.getPosition();
        if (currentPosition > document.getDocumentLength()) {return;}

        int lineIndex = findVisualLineIndex(currentPosition, visualLines);
        lineIndex = adjustForAffinity(currentPosition, lineIndex, visualLines);
        if (lineIndex == -1) return;

        VisualLine line = visualLines.get(lineIndex);

        if (line.hasNewlineChar()) {
            cursor.setPosition(line.endPosition() -1);
        } else {
            cursor.setPosition(line.endPosition());
            cursor.setAffinity(Caret.Affinity.LEFT);
        }
    }

    /**
     * Calculates the position of the cursor after vertical movement (moving to a line above (-1) or below (1))
     * @param visualLines The visual lines on the screen.
     * @param direction The line to move to, either the previous line (up) which is -1 or the next line (down) which is 1
     * @return The new cursorPosition which is the numerical position and the cursor affinity
     */
    public void moveUpOrDown(List<VisualLine> visualLines, int direction) {
        int currentPosition = cursor.getPosition();
        if (currentPosition > document.getDocumentLength()) {return;}

        int currentLineIndex = findVisualLineIndex(currentPosition, visualLines);
        currentLineIndex = adjustForAffinity(currentPosition, currentLineIndex, visualLines);

        int targetLineIndex = currentLineIndex + direction;
        if (targetLineIndex < 0 || targetLineIndex >= visualLines.size()) {return;}

        VisualLine currentLine = visualLines.get(currentLineIndex);
        VisualLine targetLine = visualLines.get(targetLineIndex);

        int currentColumn = currentPosition - currentLine.startPosition();
        int targetPosition;

        if (currentColumn >= targetLine.length()) {
            if (targetLine.hasNewlineChar()) {
                targetPosition = targetLine.startPosition() + targetLine.length() -1;
            } else {
                targetPosition = targetLine.startPosition() + targetLine.length();
                cursor.setAffinity(Caret.Affinity.LEFT);
            }
        } else {
            targetPosition = targetLine.startPosition() + currentColumn;
        }
        cursor.setPosition(targetPosition);
    }

    public void moveToClickPosition(double clickX, double clickY, List<VisualLine> visualLines) {
        int lineIndex = (int)((clickY - paddingTop) / measurer.getLineHeight());
        lineIndex = Math.max(0, Math.min(lineIndex, visualLines.size() - 1));
        VisualLine line = visualLines.get(lineIndex);

        // Convert X to column
        int column = 0;
        double width = paddingHorizontal;
        for (; column < line.length(); column++) {
            double charWidth = measurer.measureWidth(line.text().substring(column, column + 1));
            width += charWidth;
            if (width >= clickX) break;
        }

        if (width < clickX) {
            int newPos = line.startPosition() + line.length();
            moveToLineEnd(visualLines);
        } else {
            int newPos = line.startPosition() + column;
            cursor.setPosition(newPos);
            cursor.setAffinity(Caret.Affinity.RIGHT);
        }

        updateCursorLocation(visualLines);
    }


    /*private CursorPosition calculateTargetPosition( VisualLine targetLine, int desiredColumn) {
        int maxColumn = targetLine.length();

        if (desiredColumn >= maxColumn) {
            if (targetLine.hasNewlineChar()) {
                return new CursorPosition(targetLine.startPosition() + maxColumn - 1,
                        CursorModel.Affinity.RIGHT);
            } else {
                return new CursorPosition(targetLine.startPosition() + maxColumn, CursorModel.Affinity.LEFT);
            }
        } else {
            return new CursorPosition(targetLine.startPosition() + desiredColumn, CursorModel.Affinity.RIGHT);
        }
    }*/

    public int findVisualLineIndex(int position, List<VisualLine> visualLines) {
        if (visualLines.isEmpty()) return -1;

        int docLength = document.getDocumentLength();
        position = Math.max(0, Math.min(position, docLength));

        int left = 0, right = visualLines.size() - 1;
        int result = -1;

        while (left <= right) {
            int mid = left + (right - left) / 2;
            VisualLine line = visualLines.get(mid);

            if (position >= line.startPosition() && position <= line.endPosition()) {
                result = mid;
                right = mid - 1;
            }
            else if (position < line.startPosition()) {
                right = mid - 1;
            } else {
                left = mid + 1;
            }
        }
        return result != -1 ? result : Math.max(0, visualLines.size() - 1);
    }

    public int adjustForAffinity(int position, int lineIndex, List<VisualLine> visualLines) {
        VisualLine line = visualLines.get(lineIndex);

        if (position == line.startPosition()) {
            if (lineIndex == 0) return 0;
            return (cursor.getAffinity() == Caret.Affinity.RIGHT) ? lineIndex - 1 : lineIndex;
        }

        if (position == line.endPosition()) {
            if (lineIndex == visualLines.size() - 1) return lineIndex;
            return (cursor.getAffinity() == Caret.Affinity.LEFT) ? lineIndex : lineIndex + 1;
        }
        return lineIndex;
    }

    public void updateCursorLocation(List<VisualLine> visualLines) {
        if (cursor == null || visualLines.isEmpty()) {return;}

        int pos = cursor.getPosition();
        int vIndex =findVisualLineIndex(pos, visualLines);
        vIndex = adjustForAffinity(pos, vIndex, visualLines);

        if (vIndex < 0) {
            vIndex = visualLines.size() - 1;
        }

        VisualLine vline = visualLines.get(vIndex);
        int lineStart = vline.startPosition();
        int col = pos - lineStart;
        col = Math.max(0, Math.min(col, vline.length()));

        double x = (col == 0) ? paddingHorizontal :
                paddingHorizontal + measurer.measureWidth(vline.text().substring(0, col));


        double y = paddingTop + measurer.getBaselineOffset() + (vIndex * measurer.getLineHeight());

        this.cursorX = x;
        this.cursorY = y;
    }

    public double getCursorX() { return cursorX;}
    public double getCursorY() { return cursorY;}


    public int findVisualColumnIndex(int position, VisualLine visualLine) {
        return position - visualLine.startPosition();
    }


}

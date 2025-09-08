package texteditor.view.cursor;

import texteditor.model.CursorModel;
import texteditor.model.PieceTable;
import texteditor.view.layout.VisualLine;

import java.util.List;

public class CursorPositionCalculator {
    private final PieceTable document;

    public CursorPositionCalculator(PieceTable document) {
        this.document = document;
    }

    public CursorPosition calculateLeftMovement(CursorModel cursor) {
        int currentPosition = cursor.getPosition();
        if (currentPosition <= 0) {
            return new CursorPosition(0, CursorModel.Affinity.RIGHT);
        }
        return new CursorPosition(currentPosition - 1, CursorModel.Affinity.RIGHT);
    }

    public CursorPosition calculateRightMovement(CursorModel cursor) {
        int currentPosition = cursor.getPosition();
        int maxPosition = document.getDocumentLength();
        if (currentPosition >= maxPosition) {
            return new CursorPosition(maxPosition, CursorModel.Affinity.RIGHT);
        }
        return new CursorPosition(currentPosition + 1, CursorModel.Affinity.RIGHT);
    }

    public CursorPosition calculateLineStartMovement(CursorModel cursor, List<VisualLine> visualLines) {
        int currentPosition = cursor.getPosition();

        int lineIndex = findVisualLineIndex(currentPosition, cursor.getAffinity(), visualLines);
        lineIndex = adjustForAffinity(currentPosition, lineIndex, cursor.getAffinity(), visualLines);
        if (lineIndex < 0) return new CursorPosition(currentPosition, cursor.getAffinity());

        VisualLine line = visualLines.get(lineIndex);
        int startPosition = line.startPosition();

        return new CursorPosition(startPosition, CursorModel.Affinity.RIGHT);
    }

    public CursorPosition calculateLineEndMovement(CursorModel cursor, List<VisualLine> visualLines) {
        int currentPosition = cursor.getPosition();

        int lineIndex = findVisualLineIndex(currentPosition, cursor.getAffinity(), visualLines);
        lineIndex = adjustForAffinity(currentPosition, lineIndex, cursor.getAffinity(), visualLines);
        if (lineIndex < 0) return new CursorPosition(currentPosition, cursor.getAffinity());

        VisualLine line = visualLines.get(lineIndex);
        int endPosition = line.endPosition();

        if (line.hasNewlineChar()) {
            return new CursorPosition(endPosition - 1, CursorModel.Affinity.RIGHT);
        } else {
            return new CursorPosition(endPosition, CursorModel.Affinity.LEFT);
        }
    }

    /**
     * Calculates the position of the cursor after vertical movement (moving to a line above (-1) or below (1))
     * @param cursor The cursor object.
     * @param visualLines The visual lines on the screen.
     * @param direction The line to move to, either the previous line (up) which is -1 or the next line (down) which is 1
     * @return The new cursorPosition which is the numerical position and the cursor affinity
     */
    public CursorPosition calculateVerticalMovement(CursorModel cursor, List<VisualLine> visualLines, int direction) {
        int currentPosition = cursor.getPosition();

        int currentLineIndex = findVisualLineIndex(currentPosition, cursor.getAffinity(), visualLines);
        currentLineIndex = adjustForAffinity(currentPosition, currentLineIndex, cursor.getAffinity(), visualLines);
        if (currentLineIndex < 0) return new CursorPosition(currentPosition, cursor.getAffinity());

        int targetLineIndex = currentLineIndex + direction;
        if (targetLineIndex < 0 || targetLineIndex >= visualLines.size()) {
            return new CursorPosition(currentPosition, cursor.getAffinity());
        }
        VisualLine currentLine = visualLines.get(currentLineIndex);
        VisualLine targetLine = visualLines.get(targetLineIndex);

        int currentColumn = currentPosition - currentLine.startPosition();

        return calculateTargetPosition(targetLine, currentColumn);
    }

    private CursorPosition calculateTargetPosition( VisualLine targetLine, int desiredColumn) {
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
    }

    public int findVisualLineIndex(int position, CursorModel.Affinity affinity, List<VisualLine> visualLines) {
        if (visualLines.isEmpty()) return -1;

        int docLength = document.getDocumentLength();
        position = Math.max(0, Math.min(position, docLength));

        for (int i = 0; i < visualLines.size(); i++) {
            VisualLine line = visualLines.get(i);
            int start = line.startPosition();
            int end = line.endPosition();

            if (position >= start && position <= end) {
                return i;
            }
        }
        return Math.max(0, visualLines.size() - 1);
    }

    /*
    public int findLineIndex(int position, List<VisualLine> visualLines) {
        if (visualLines.isEmpty()) return -1;

        int docLength = document.getDocumentLength();
        position = Math.max(0, Math.min(position, docLength));

        int left = 0, right = visualLines.size() - 1;
        while (left <= right) {
            int mid = left + (right - left) / 2;
            VisualLine line = visualLines.get(mid);

            if (position > line.endPosition()) {
                left = mid + 1;
            }
            else if (position < line.startPosition()) {
                right = mid - 1;
            } else {
                return mid;
            }
        }
        return Math.max(0, visualLines.size() - 1);
    }

     */

    public int adjustForAffinity(int position, int lineIndex, CursorModel.Affinity affinity, List<VisualLine> visualLines) {
        VisualLine line = visualLines.get(lineIndex);

        if (position == line.startPosition()) {
            if (lineIndex == 0) return 0;
            return (affinity == CursorModel.Affinity.RIGHT) ? lineIndex - 1 : lineIndex;
        }

        if (position == line.endPosition()) {
            if (lineIndex == visualLines.size() - 1) return lineIndex;
            return (affinity == CursorModel.Affinity.LEFT) ? lineIndex : lineIndex + 1;
        }
        return lineIndex;
    }


    public int findVisualColumnIndex(int position, VisualLine visualLine) {
        return position - visualLine.startPosition();
    }

    public record CursorPosition(int position, CursorModel.Affinity affinity) {
        public CursorPosition {
            if (position < 0) throw new IllegalArgumentException("Position cannot be negative");
            if (affinity == null) throw new IllegalArgumentException("Affinity cannot be null");
        }
    }
}

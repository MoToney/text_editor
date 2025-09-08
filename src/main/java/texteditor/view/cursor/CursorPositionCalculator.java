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

    public int findVisualLineIndex(int position, CursorModel.Affinity affinity, List<VisualLine> visualLines) {
        if (visualLines.isEmpty()) return -1;

        int docLength = document.getDocumentLength();
        position = Math.max(0, Math.min(position, docLength));

        for (int i = 0; i < visualLines.size(); i++) {
            VisualLine line = visualLines.get(i);
            int start = line.startPosition();
            int end = line.endPosition();

            if (position > start && position < end) {
                return i;
            }

            if (position == start) {
                if (i == 0) return 0;
                return (affinity == CursorModel.Affinity.RIGHT) ? i - 1 : i;
            }

            if (position == end) {
                if (i == visualLines.size() - 1) return i;
                return (affinity == CursorModel.Affinity.LEFT) ? i : i + 1;
            }
        }
        return Math.max(0, visualLines.size() - 1);
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

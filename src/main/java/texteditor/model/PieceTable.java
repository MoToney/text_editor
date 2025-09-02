package texteditor.model;

import java.util.*;

public class PieceTable {
    private class Piece {
        boolean isOriginal;
        int start;
        int length;

        Piece(boolean isOriginal, int start, int length) {
            this.isOriginal = isOriginal;
            this.start = start;
            this.length = length;
        }

        @Override
        public String toString() {
            String fragment;
            if (isOriginal) {
                fragment = originalBuffer.substring(start, start + length);
            } else {
                fragment = addBuffer.substring(start, start + length);
            }
            return String.format(
                    "Piece(isOriginal=%b, start=%d, length=%d, text='%s')",
                    isOriginal, start, length, fragment
            );
        }
    }

    private class Line {
        int startPieceIndex;
        int startOffsetInPiece;
        int length;

        Line(int startPieceIndex, int startOffsetInPiece, int length) {
            this.startPieceIndex = startPieceIndex;
            this.startOffsetInPiece = startOffsetInPiece;
            this.length = length;
        }

        @Override
        public String toString() {
            return String.format("Line(startPiece=%d, startOffset=%d, length=%d)", startPieceIndex, startOffsetInPiece, length);
        }
    }




    private final String originalBuffer;
    private final StringBuilder addBuffer;
    private final List<Piece> pieces;
    private final List<Line> lineCache;
    private int totalLength;

    public PieceTable(String originalText) {
        this.originalBuffer = originalText;
        this.addBuffer = new StringBuilder();
        this.pieces = new ArrayList<>();
        this.lineCache = new ArrayList<>();

        if (!originalText.isEmpty()) {
            pieces.add(new Piece(true, 0, originalText.length()));
            this.totalLength = originalText.length();
        }

        rebuildLineCache();
    }

    private void rebuildLineCache() {
        lineCache.clear();
        if (pieces.isEmpty()) {
            lineCache.add(new Line(0,0,0));
            return;
        }

        int currentPieceIndex = 0;
        int currentOffsetInPiece = 0;
        int lineStartPieceIndex = 0;
        int lineStartOffsetInPiece = 0;
        int lineLength = 0;

        while (currentPieceIndex < pieces.size()) {
            Piece p = pieces.get(currentPieceIndex);
            String buffer = p.isOriginal ? originalBuffer : addBuffer.toString();

            for (int i = 0; i < p.length; i++) {
                char c = buffer.charAt(p.start + i);
                lineLength++;
                currentOffsetInPiece++;

                if (c == '\n') {
                    lineCache.add(new Line(lineStartPieceIndex, lineStartOffsetInPiece, lineLength));
                    lineStartPieceIndex = currentPieceIndex;
                    lineStartOffsetInPiece = currentOffsetInPiece;
                    lineLength = 0;
                }
            }
            currentPieceIndex++;
            currentOffsetInPiece = 0;
        }
        if (lineLength > 0 || lineCache.isEmpty()) {
            lineCache.add(new Line(lineStartPieceIndex, lineStartOffsetInPiece, lineLength));
        }
    }

    public int getLineCount() {
        return this.lineCache.size();
    }

    public int getLineIndex(int position) {
        if (position < 0 || position > totalLength) {
            throw new IndexOutOfBoundsException("Position is out of bounds: " + position);
        }

        int runningTotal = 0;
        for (int i = 0; i < lineCache.size(); i++) {
            Line line = lineCache.get(i);
            if (position < runningTotal + line.length) {
                return i;
            }
            runningTotal += line.length;
        }
        return lineCache.size() - 1;
    }

    public int getColumnIndex(int position) {
        int lineIndex = getLineIndex(position);
        int runningTotal = 0;

        for (int i = 0; i < lineIndex; i++) {
            runningTotal += lineCache.get(i).length;
        }

        return position - runningTotal;
    }

    public int getPosition(int lineIndex, int columnIndex) {
        int position = 0;
        for (int i = 0; i < lineIndex; i++) {
            position += lineCache.get(i).length;
        }
        position += columnIndex;
        return position;
    }

    public int getPositionAtEndOfLine(int lineIndex) {
        int position = 0;
        for (int i = 0; i <= lineIndex; i++) {
            position += lineCache.get(i).length;
        }
        return position;
    }

    public int getPositionAtStartOfLine(int lineIndex) {
        int position = 0;
        if (lineIndex > 0) position = getPositionAtEndOfLine(lineIndex - 1);
        return position;
    }

    public int getLineLength(int lineIndex) {
        return getLine(lineIndex).length();
    }

    public int getRemainingLineLength(int position) {
        int lineIndex = getLineIndex(position);
        int columnIndex = getColumnIndex(position);
        int lineLength = getLineLength(lineIndex);

        return lineLength - columnIndex;
    }

    public boolean isLastLine(int lineIndex) {
        return lineIndex == getLineCount() - 1;
    }

    public String getLine(int lineIndex) {
        if (lineIndex < 0 || lineIndex >= lineCache.size()) {
            throw new IndexOutOfBoundsException("Line index out of bounds: " + lineIndex);
        }

        Line lineInfo = lineCache.get(lineIndex);
        if (lineInfo.length == 0) return "";

        StringBuilder lineBuilder = new StringBuilder(lineInfo.length);
        int remainingLength = lineInfo.length;

        int currentPieceIndex = lineInfo.startPieceIndex;
        int offset = lineInfo.startOffsetInPiece;

        while (remainingLength > 0 && currentPieceIndex < pieces.size()) {
            Piece p = pieces.get(currentPieceIndex);
            String buffer = p.isOriginal ? originalBuffer : addBuffer.toString();

            int charsToReadFromPiece = Math.min(remainingLength, p.length - offset);
            lineBuilder.append(buffer, p.start + offset, p.start + offset + charsToReadFromPiece);

            remainingLength -= charsToReadFromPiece;
            currentPieceIndex++;
            offset = 0;
        }

        String lineText = lineBuilder.toString();
        if (lineText.endsWith("\n")) {
            return lineText.substring(0, lineText.length() - 1);
        }
        return lineText;
    }


    public String getText() {
        StringBuilder result = new StringBuilder();
        for (Piece piece : pieces) {
            if (piece.isOriginal) {
                result.append(originalBuffer.substring(piece.start, piece.start + piece.length));
            } else {
                result.append(addBuffer.substring(piece.start, piece.start + piece.length));
            }
        }
        return result.toString();
    }

    public int getLength() {
        return this.totalLength;
    }

    public record PieceInfo(Piece piece, int localIndex, int pieceListIndex) {}

    public PieceInfo getPieceByIndex(int index) {
        int currentLength = 0;
        for (int i = 0; i < pieces.size(); i++) {
            currentLength += pieces.get(i).length;
            if (currentLength >= index) {
                int localIndex = pieces.get(i).length - (currentLength - index);
                return new PieceInfo(pieces.get(i), localIndex, i);
            }
        }
        return null;
    }

    public void insertText(int index, String text) {
        // 1. Insert new text into the add buffer and create a piece
        Piece textPiece = new Piece(false, addBuffer.length(), text.length());
        this.addBuffer.append(text);

        PieceInfo pieceInfo = getPieceByIndex(index);

        if (pieceInfo != null) {
            Piece originalPiece = pieceInfo.piece;
            int localIndex = pieceInfo.localIndex;
            int i = pieceInfo.pieceListIndex;

            Piece beforePiece = (localIndex > 0)
                    ? new Piece(originalPiece.isOriginal, originalPiece.start, localIndex)
                    : null;

            Piece afterPiece = ((originalPiece.length - localIndex) > 0)
                ? new Piece(originalPiece.isOriginal, originalPiece.start + localIndex, originalPiece.length - localIndex)
                    : null;

            pieces.remove(originalPiece);

            if (beforePiece != null) {
                pieces.add(i, beforePiece);
                i++;
            }

            pieces.add(i, textPiece);
            i++; // Increment for the next piece

            if (afterPiece != null) {
                pieces.add(i, afterPiece);
            }
            this.totalLength += text.length();
            rebuildLineCache();
            return;
        }
        // 2. Find which piece the index falls into
        pieces.add(textPiece);
        this.totalLength += text.length();
        rebuildLineCache();
    }

    public void removeText(int index, int deleteLength) {
        PieceInfo pieceInfo = getPieceByIndex(index);

        if (pieceInfo != null) {
            Piece originalPiece = pieceInfo.piece;
            int localIndex = pieceInfo.localIndex;
            int i = pieceInfo.pieceListIndex;

            int restInFirst = originalPiece.length - localIndex; // chars from localIndex to end
            Piece beforePiece = (localIndex > 0)
                    ? new Piece(originalPiece.isOriginal, originalPiece.start, localIndex)
                    : null;

            // Case 1: delete fits entirely inside the first piece
            if (deleteLength < restInFirst) {
                Piece afterPiece = new Piece(
                        originalPiece.isOriginal,
                        originalPiece.start + localIndex + deleteLength,
                        restInFirst - deleteLength
                );

                pieces.remove(i);                       // replace original
                if (beforePiece != null) pieces.add(i++, beforePiece);
                pieces.add(i, afterPiece);
                this.totalLength -= deleteLength;
                rebuildLineCache();
                return;
            }

            // Case 2: delete exactly to the end of the first piece
            if (deleteLength == restInFirst) {
                pieces.remove(i);
                if (beforePiece != null) pieces.add(i, beforePiece);
                this.totalLength -= deleteLength;
                rebuildLineCache();
                return;
            }

            // Case 3: delete spans multiple pieces
            int remaining = deleteLength - restInFirst;
            pieces.remove(i);
            if (beforePiece != null) pieces.add(i++, beforePiece);

            while (remaining > 0 && i < pieces.size()) {
                Piece currentPiece = pieces.get(i);
                if (remaining > currentPiece.length) {
                    pieces.remove(i);
                    remaining -= currentPiece.length;
                }
                else if (remaining == currentPiece.length) {
                    pieces.remove(i);
                    this.totalLength -= deleteLength;
                    rebuildLineCache();
                    return;
                }
                else {
                    Piece notDeletedPiecePortion = new Piece(currentPiece.isOriginal, currentPiece.start + remaining, currentPiece.length - remaining);
                    pieces.remove(i);
                    pieces.add(i, notDeletedPiecePortion);
                    this.totalLength -= deleteLength;
                    rebuildLineCache();
                    return;
                }
            }

        }
    }

    public static void main(String[] args) {
        PieceTable testTable = new PieceTable("Does this work ");
        String text = testTable.getText();
        System.out.println(text);

        testTable.insertText(10, "really ");
        String newtext = testTable.getText();
        System.out.println(newtext);

        testTable.insertText(21, " wow ");
        String newtext2 = testTable.getText();
        System.out.println(newtext2);

        testTable.insertText(10, "thing \n");
        String newtext3 = testTable.getText();
        System.out.println(newtext3);

        testTable.removeText(10, 5);
        System.out.println(testTable.getText());



        System.out.println(testTable.getLength());

        System.out.println(testTable.getLineCount());

        System.out.println(testTable.getLine(0));


    }
}


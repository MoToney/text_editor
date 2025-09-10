package texteditor.model;

import java.util.*;

public class PieceTable {
    private class Piece {
        int start;
        int length;
        BufferType source;

        enum BufferType {
            ORIGINAL,
            ADD
        }

        Piece(BufferType source, int start, int length) {
            this.source = source;
            this.start = start;
            this.length = length;
        }

        @Override
        public String toString() {
            String fragment = getText();
            return String.format(
                    "Piece(source=%s, start=%d, length=%d, text='%s')",
                    source, start, length, fragment
            );
        }

        public String getText() {
            if (source == BufferType.ORIGINAL) {
                return originalBuffer.substring(start, start + length);
            } else {
                return addBuffer.substring(start, start + length);
            }
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

        if (!originalText.isEmpty() && originalText != null) {
            pieces.add(new Piece(Piece.BufferType.ORIGINAL,0, originalText.length()));
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
            String bufferContent = (p.source == Piece.BufferType.ORIGINAL) ? originalBuffer : addBuffer.toString();

            for (int i = 0; i < p.length; i++) {
                char c = bufferContent.charAt(p.start + i);
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

    public int getLineLength(int lineIndex) {
        return getLine(lineIndex).length();
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
        int offsetInPiece = lineInfo.startOffsetInPiece;

        while (remainingLength > 0 && currentPieceIndex < pieces.size()) {
            Piece p = pieces.get(currentPieceIndex);
            String bufferContent = (p.source == Piece.BufferType.ORIGINAL) ? originalBuffer : addBuffer.toString();
            int charsToRead = Math.min(remainingLength, p.length - offsetInPiece);

            lineBuilder.append(bufferContent, p.start + offsetInPiece, p.start + offsetInPiece + charsToRead);

            remainingLength -= charsToRead;
            currentPieceIndex++;
            offsetInPiece = 0;
        }
        return lineBuilder.toString();
    }


    public String getText() {
        StringBuilder result = new StringBuilder();
        for (Piece piece : pieces) {
            result.append(piece.getText());
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

    public String getTextByIndex(int index, int length) {
        return "";
    }

    public void insertText(int index, String text) {
        // 1. Insert new text into the add buffer and create a piece
        Piece textPiece = new Piece(Piece.BufferType.ADD, addBuffer.length(), text.length());
        this.addBuffer.append(text);

        PieceInfo pieceInfo = getPieceByIndex(index);

        if (pieceInfo != null) {
            Piece originalPiece = pieceInfo.piece;
            int localIndex = pieceInfo.localIndex;
            int i = pieceInfo.pieceListIndex;

            Piece beforePiece = (localIndex > 0)
                    ? new Piece(originalPiece.source, originalPiece.start, localIndex)
                    : null;

            Piece afterPiece = ((originalPiece.length - localIndex) > 0)
                ? new Piece(originalPiece.source, originalPiece.start + localIndex, originalPiece.length - localIndex)
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
                    ? new Piece(originalPiece.source, originalPiece.start, localIndex)
                    : null;

            // Case 1: delete fits entirely inside the first piece
            if (deleteLength < restInFirst) {
                Piece afterPiece = new Piece(
                        originalPiece.source,
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
                    Piece notDeletedPiecePortion = new Piece(currentPiece.source, currentPiece.start + remaining, currentPiece.length - remaining);
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


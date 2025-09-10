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

    public record TextLocation(Piece piece, int offsetInPiece, int pieceIndex) {}

    public TextLocation findLocationAt(int position) {
        int currentLength = 0;
        for (int i = 0; i < pieces.size(); i++) {
            currentLength += pieces.get(i).length;
            if (currentLength >= position) {
                int localIndex = pieces.get(i).length - (currentLength - position);
                return new TextLocation(pieces.get(i), localIndex, i);
            }
        }
        return null;
    }

    public String getTextSpan(int position, int length) {
        return "";
    }

    public void insert(int position, String text) {
        if (text == null || text.isEmpty()) return;

        int textLength = text.length();
        addBuffer.append(text);
        Piece newPiece = new Piece(Piece.BufferType.ADD, addBuffer.length() - textLength, textLength);

        if (pieces.isEmpty()) {
            pieces.add(newPiece);
            totalLength += textLength;
            rebuildLineCache();
            return;
        }

        TextLocation insertionPoint = findLocationAt(position);
        if (insertionPoint == null) return;

        Piece pieceToSplit = insertionPoint.piece;
        int splitOffset = insertionPoint.offsetInPiece;
        int targetIndex = insertionPoint.pieceIndex;

        if (splitOffset == 0) {
            pieces.add(targetIndex, newPiece);
        }
        else if (splitOffset == pieceToSplit.length) {
            pieces.add(targetIndex + 1, newPiece);
        } else {
            Piece leftPart = new Piece(pieceToSplit.source, pieceToSplit.start, splitOffset);
            Piece rightPart = new Piece(pieceToSplit.source, pieceToSplit.start + splitOffset, pieceToSplit.length - splitOffset);

            pieces.remove(targetIndex);
            pieces.add(targetIndex, leftPart);
            pieces.add(targetIndex + 1, newPiece);
            pieces.add(targetIndex + 2, rightPart);

        }
        this.totalLength += text.length();
        rebuildLineCache();
    }

    public void remove(int position, int length) {
        if (length <= 0 || position < 0 || position + length > this.totalLength) return;

        TextLocation startLocation = findLocationAt(position);
        TextLocation endLocation = findLocationAt(position + length);

        if (startLocation.pieceIndex() == endLocation.pieceIndex()) {
            Piece pieceToModify = startLocation.piece();
            int pieceIndex = startLocation.pieceIndex();

            int startOffset = startLocation.offsetInPiece();
            int endOffset = endLocation.offsetInPiece();

            pieces.remove(pieceIndex);

            Piece leftPart = (startOffset > 0)
                    ? new Piece(pieceToModify.source, pieceToModify.start, startOffset)
                    : null;

            Piece rightPart = (endOffset < pieceToModify.length)
                    ? new Piece(pieceToModify.source, pieceToModify.start + endOffset,
                    pieceToModify.length - endOffset)
                    : null;

            int currentIndex = pieceIndex;
            if (leftPart != null) pieces.add(currentIndex++, leftPart);
            if (rightPart != null) pieces.add(currentIndex, rightPart);
        }
        else {
            Piece firstPiece = startLocation.piece();
            int firstPieceIndex = startLocation.pieceIndex();
            int firstPieceStartOffset = startLocation.offsetInPiece();

            Piece lastPiece = endLocation.piece();
            int lastPieceIndex = endLocation.pieceIndex();
            int lastPieceEndOffset = endLocation.offsetInPiece();

            // remove all middle pieces that are fully consumed when deleting
            for (int i = lastPieceIndex - 1; i > firstPieceIndex; i--) {
                pieces.remove(i);
            }

            if (lastPieceEndOffset < lastPiece.length) {
                Piece rightPart = new Piece(lastPiece.source, lastPiece.start + lastPieceEndOffset,
                        lastPiece.length - lastPieceEndOffset);
                pieces.set(firstPieceIndex + 1, rightPart);
            } else {
                pieces.remove(firstPieceIndex + 1);
            }

            if (firstPieceStartOffset > 0) {
                Piece leftPart = new Piece(firstPiece.source, firstPiece.start, firstPieceStartOffset);
                pieces.set(firstPieceIndex, leftPart);
            } else {
                pieces.remove(firstPieceIndex);
            }
        }
        totalLength -= length;
        rebuildLineCache();
    }

    public static void main(String[] args) {
        PieceTable testTable = new PieceTable("Does this work ");
        String text = testTable.getText();
        System.out.println(text);

        testTable.insert(10, "really ");
        String newtext = testTable.getText();
        System.out.println(newtext);

        testTable.insert(21, " wow ");
        String newtext2 = testTable.getText();
        System.out.println(newtext2);

        testTable.insert(10, "thing \n");
        String newtext3 = testTable.getText();
        System.out.println(newtext3);

        testTable.remove(10, 5);
        System.out.println(testTable.getText());

        System.out.println(testTable.getLength());

        System.out.println(testTable.getLineCount());

        System.out.println(testTable.getLine(0));


    }
}


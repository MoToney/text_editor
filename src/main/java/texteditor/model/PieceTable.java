package texteditor.model;

import java.util.*;

public class PieceTable {

    private final String originalBuffer;
    private final StringBuilder addBuffer;
    // private final List<Piece> pieces;
    private final PieceTree pieceTree;
    private final List<Line> lineCache;
    private int totalLength;

    public PieceTable(String originalText) {
        this.originalBuffer = originalText;
        this.addBuffer = new StringBuilder();
        // this.pieces = new ArrayList<>();
        this.lineCache = new ArrayList<>();
        this.pieceTree = new PieceTree();

        if (!originalText.isEmpty() && originalText != null) {
            Piece piece = new Piece(Piece.BufferType.ORIGINAL, 0, originalText.length());
            pieceTree.insert(0, piece);
            this.totalLength = piece.getLength();
        }

        rebuildLineCache();
    }

    public void insert(int position, String text) {
        if (text == null || text.isEmpty()) return;

        int textLength = text.length();
        addBuffer.append(text);
        Piece newPiece = new Piece(Piece.BufferType.ADD, addBuffer.length() - textLength, textLength);

        pieceTree.insert(position, newPiece);
        totalLength += text.length();
        rebuildLineCache();
    }


    public void remove(int position, int length) {
        if (length <= 0 || position < 0 || position >= totalLength) return;

        if (position + length > totalLength) {
            length = totalLength - position;  // trim to valid range
        }

        pieceTree.remove(position, length);  // delegate to PieceTree
        totalLength -= length;

        rebuildLineCache();
    }

    public String getText() {
        return pieceTree.getText(originalBuffer, addBuffer);
    }

    public int getLength() {
        return pieceTree.length();
    }



    public record TextLocation(Piece piece, int offsetInPiece, int pieceIndex) {}
    private void rebuildLineCache() {
        List<Piece> pieces = pieceTree.toPieceList();
        lineCache.clear();
        if (pieces.isEmpty()) {
            lineCache.add(new Line(0, 0, 0));
            return;
        }

        int currentLineStartPiece = 0;
        int currentLineStartOffset = 0;
        int currentLineLength = 0;

        for (int pieceIndex = 0; pieceIndex < pieces.size(); pieceIndex++) {
            Piece piece = pieces.get(pieceIndex);
            List<Integer> lineStarts = piece.getLineStarts(originalBuffer, addBuffer);

            if (lineStarts.size() <= 1) {
                currentLineLength += piece.getLength();
            } else {
                for (int i = 1; i < lineStarts.size(); i++) {
                    int lineStartInPiece = (i == 1) ? 0 : lineStarts.get(i - 1);
                    int lineEndInPiece = lineStarts.get(i) - 1;
                    int segmentLength = lineEndInPiece - lineStartInPiece + 1;

                    currentLineLength += segmentLength;

                    lineCache.add(new Line(currentLineStartPiece, currentLineStartOffset, currentLineLength));

                    currentLineStartPiece = pieceIndex;
                    currentLineStartOffset = lineStarts.get(i);
                    currentLineLength = 0;

                }

                int lastNewlinePos = lineStarts.get(lineStarts.size() - 1);
                if (lastNewlinePos < piece.getLength()) {
                    currentLineLength += piece.getLength() - lastNewlinePos;
                }
            }
        }
        if (currentLineLength > 0 || lineCache.isEmpty()) {
            lineCache.add(new Line(currentLineStartPiece, currentLineStartOffset, currentLineLength));
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
        List<Piece> pieces = pieceTree.toPieceList();
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
            String bufferContent = (p.getSource() == Piece.BufferType.ORIGINAL) ? originalBuffer : addBuffer.toString();
            int charsToRead = Math.min(remainingLength, p.getLength() - offsetInPiece);

            lineBuilder.append(bufferContent, p.getStart() + offsetInPiece, p.getStart() + offsetInPiece + charsToRead);

            remainingLength -= charsToRead;
            currentPieceIndex++;
            offsetInPiece = 0;
        }
        return lineBuilder.toString();
    }
}


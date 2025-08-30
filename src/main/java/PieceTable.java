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


    private final String originalBuffer;
    private final StringBuilder addBuffer;
    private final List<Piece> pieces;

    public PieceTable(String originalText) {
        this.originalBuffer = originalText;
        this.addBuffer = new StringBuilder();
        this.pieces = new ArrayList<>();

        if (!originalText.isEmpty()) {
            pieces.add(new Piece(true, 0, originalText.length()));
        }
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

    public record PieceInfo(Piece piece, int localIndex, int pieceListIndex) {
    }

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
            return;
        }
        // 2. Find which piece the index falls into
        pieces.add(textPiece);
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
                return;
            }

            // Case 2: delete exactly to the end of the first piece
            if (deleteLength == restInFirst) {
                pieces.remove(i);
                if (beforePiece != null) pieces.add(i, beforePiece);
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
                    return;
                }
                else {
                    Piece notDeletedPiecePortion = new Piece(currentPiece.isOriginal, currentPiece.start + remaining, currentPiece.length - remaining);
                    pieces.remove(i);
                    pieces.add(i, notDeletedPiecePortion);
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

        testTable.insertText(10, "thing ");
        String newtext3 = testTable.getText();
        System.out.println(newtext3);

        testTable.removeText(10, 5);
        System.out.println(testTable.getText());


    }
}


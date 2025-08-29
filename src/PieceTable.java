package src;

import java.util.ArrayList;
import java.util.List;

public class PieceTable {

    private static class Piece {
        boolean isOriginal;
        int start;
        int length;

        Piece(boolean isOriginal, int start, int length) {
            this.isOriginal = isOriginal;
            this.start = start;
            this.length = length;
        }

        public String toString() {
            return String.format("Piece(isOriginal=%b, start=%d, length=%d)", isOriginal, start, length);
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
            pieces.add(new Piece(true, 0,originalText.length()));
        }
    }

    public String getText() {
        StringBuilder result = new StringBuilder();
        for (Piece piece : pieces) {
            if (piece.isOriginal) {
                result.append(originalBuffer.substring(piece.start, piece.start + piece.length));
            }
            else {
                result.append(addBuffer.substring(piece.start, piece.start + piece.length));
            }
        }
        return result.toString();
    }

    public String insertText(int index, String text) {
        // 1. Insert new text into the add buffer and create a piece
        Piece textPiece = new Piece(false, addBuffer.length(),text.length());
        this.addBuffer.append(text);

        // 2. Find which piece the index falls into
        int currentLength = 0;
        for (int i = 0; i < pieces.size(); i++) {
            currentLength += pieces.get(i).length;
            if (currentLength >= index) {
                int localIndex = pieces.get(i).length - (currentLength - index);
                Piece beforePiece = new Piece(pieces.get(i).isOriginal, pieces.get(i).start, localIndex);
                Piece afterPiece = new Piece(pieces.get(i).isOriginal, localIndex, pieces.get(i).length - localIndex);
                pieces.remove(pieces.get(i));
                pieces.add(i, beforePiece);
                pieces.add(i + 1, textPiece);
                pieces.add(i + 2, afterPiece);
                return pieces.toString();
            }
        }
        pieces.add(textPiece);
        return pieces.toString();
    }

    /*public static void main(String[] args) {
        PieceTable testTable = new PieceTable("Does this work");
        String text = testTable.getText();
        System.out.println(text);

        testTable.insertText(10, "really ");
        String newtext = testTable.getText();
        System.out.println(newtext);

        testTable.insertText(21, " wow!");
        String newtext2 = testTable.getText();
        System.out.println(newtext2);

        testTable.insertText(10, "thing ");
        String newtext3 = testTable.getText();
        System.out.println(newtext3);

    }*/
}


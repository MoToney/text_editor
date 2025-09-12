package texteditor.model;

public class Line {
    public final int startPieceIndex;
    public final int startOffsetInPiece;
    public final int length;


    public Line(int startPieceIndex, int startOffsetInPiece, int length) {
        this.startPieceIndex = startPieceIndex;
        this.startOffsetInPiece = startOffsetInPiece;
        this.length = length;
    }


    @Override
    public String toString() {
        return String.format("Line(startPiece=%d, startOffset=%d, length=%d)", startPieceIndex, startOffsetInPiece, length);
    }
}

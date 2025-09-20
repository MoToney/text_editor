package texteditor.model;

public class TextBuffer {
    private final StringBuilder addBuffer;
    private final String originalBuffer;

    public TextBuffer(String originalText) {
        this.originalBuffer = originalText;
        this.addBuffer = new StringBuilder();
    }

    public String getOriginalBuffer() {
        return originalBuffer;
    }

    public String getAddBuffer() {
        return addBuffer.toString();
    }

    public void append(String text) {
        addBuffer.append(text);
    }

    public int addBufferLength() {
        return addBuffer.length();
    }

    public int originalBufferLength() {
        return originalBuffer.length();
    }

    public String getText(Piece piece) {
        if (piece.getLength() == 0) return "";
        if (piece.getSource() == Piece.BufferType.ORIGINAL) {
            return originalBuffer.substring(piece.getStart(), piece.getStart() + piece.getLength());
        } else {
            return addBuffer.substring(piece.getStart(), piece.getStart() + piece.getLength());
        }
    }
}

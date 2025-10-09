package texteditor.model;

import java.util.ArrayList;
import java.util.List;

public class Piece {

    private final Buffer buffer;
    private final int start;
    private final int length;
    private Integer lineCount;
    private boolean lineCountCalculated;
    List<Integer> lineStarts;


    Piece(Buffer buffer, int start, int length) {
        this.buffer = buffer;
        this.start = start;
        this.length = length;
        this.lineCount = null;
    }

    public Buffer getBuffer() {return buffer;}
    public int getStart() {return start;}
    public int getLength() {return length;}
    public int getEnd() { return start + length; }

    @Override
    public String toString() {
        return "Piece[start=" + start + ", length=" + length + ", text=\""
                + buffer.substring(start, start + length).replace("\n", "\\n") + "\"]";
    }


    public String getText() {
        return buffer.substring(start, start + this.length);
    }

    public String subString(int localStart, int localEnd) {
        if (localStart < 0 ||  localEnd > this.length || localStart > localEnd) {
            throw new IndexOutOfBoundsException(
                    "Invalid range: [" + localStart + ", " + localEnd +
                            ") for piece length " + this.length);
        }

        int bufferStart = this.start + localStart;
        int bufferEnd = this.start + localEnd;

        return buffer.substring(bufferStart, bufferEnd);
    }

    public char getChar(int index) {
        if (index < 0 || index >= length) throw new IndexOutOfBoundsException();
        return buffer.charAt(start + index);
    }

    public void calculateLineCount() {
        int count = 0;
        for (int i = start; i < start + length; i++) {
            if (buffer.charAt(i) == '\n') count++;
        }
        this.lineCount = count;
    }

    public Integer getLineCount() {
        if (lineCount == null) calculateLineCount();
        return this.lineCount;
    }
}

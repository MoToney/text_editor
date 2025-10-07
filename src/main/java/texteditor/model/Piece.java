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

    @Override
    public String toString() {
        return String.format(
                "Piece(buffer=%s, startLocation=%d, length=%d)",
                buffer, start, length
        );
    }

    public String getText() {
        return buffer.substring(start, start + this.length);
    }

    public String getSubString(int start, int length) {
        return buffer.substring(start, start + length);
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

    public List<Integer> getLineStarts() {
        List<Integer> starts = new ArrayList<>();
        starts.add(0);
        for (int i = start; i < start + length; i++) {
            if (buffer.charAt(i) == '\n') starts.add(i - start + 1);
        }
            return starts;
    }
}

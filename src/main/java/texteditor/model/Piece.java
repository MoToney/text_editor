package texteditor.model;

import java.util.ArrayList;
import java.util.List;

public class Piece {
    enum BufferType {
        ORIGINAL,
        ADD
    }

    private final BufferType source;
    private final int start;
    private final int length;
    private Integer lineCount;
    private boolean lineCountCalculated;
    List<Integer> lineStarts;


    Piece(BufferType source, int start, int length) {
        this.source = source;
        this.start = start;
        this.length = length;
        this.lineCount = null;
    }

    public BufferType getSource() {return source;}
    public int getStart() {return start;}
    public int getLength() {return length;}

    @Override
    public String toString() {
        return String.format(
                "Piece(source=%s, start=%d, length=%d)",
                source, start, length
        );
    }

    public String getText(TextBuffer textBuffer) {
        if (length == 0) return "";
        if (source == BufferType.ORIGINAL) {
            return textBuffer.getOriginalBuffer().substring(start, start + length);
        } else {
            return textBuffer.getAddBuffer().substring(start, start + length);
        }
    }

    public void calculateLineCount(TextBuffer textBuffer) {
        int count = 0;
        String buffer = (source == BufferType.ORIGINAL) ? textBuffer.getOriginalBuffer() : textBuffer.getAddBuffer();
        for (int i = start; i < start + length; i++) {
            if (buffer.charAt(i) == '\n') count++;
        }
        this.lineCount = count;
    }

    public Integer getLineCount(TextBuffer textBuffer) {
        if (lineCount == null) calculateLineCount(textBuffer);
        return this.lineCount;
    }

    public List<Integer> getLineStarts(TextBuffer textBuffer) {
        List<Integer> starts = new ArrayList<>();
        String buffer = (source == BufferType.ORIGINAL) ? textBuffer.getOriginalBuffer() : textBuffer.getAddBuffer();
        starts.add(0);
        for (int i = start; i < start + length; i++) {
            if (buffer.charAt(i) == '\n') starts.add(i - start + 1);
        }
            return starts;
    }
}

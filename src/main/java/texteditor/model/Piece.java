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
    List<Integer> lineStarts;


    Piece(BufferType source, int start, int length) {
        this.source = source;
        this.start = start;
        this.length = length;
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

    public String getText(String originalBuffer, StringBuilder addBuffer) {
        if (length == 0) return "";
        if (source == BufferType.ORIGINAL) {
            return originalBuffer.substring(start, start + length);
        } else {
            return addBuffer.substring(start, start + length);
        }
    }

    public List<Integer> getLineStarts(String originalBuffer, StringBuilder addBuffer) {
        List<Integer> starts = new ArrayList<>();
        String buffer = (source == BufferType.ORIGINAL) ? originalBuffer : addBuffer.toString();
        starts.add(0);
        for (int i = start; i < start + length; i++) {
            if (buffer.charAt(i) == '\n') starts.add(i - start + 1);
        }
        return starts;
    }
}

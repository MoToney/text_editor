package texteditor.view.layout;

import texteditor.model.PieceTable;

public class VisualLine {
    private final String text;
    private final int startPosition;
    private final boolean hasNewlineChar;


    public VisualLine(String text, int startPosition, boolean hasNewlineChar) {
        this.text = text != null ? text : "";
        this.startPosition = Math.max(0, startPosition);
        this.hasNewlineChar = hasNewlineChar;
    }

    public String text() {return text;}

    public int startPosition() {return startPosition;}

    public boolean hasNewlineChar() {return hasNewlineChar;}

    public int length() {return text.length();}

    public int endPosition() {return startPosition + text.length();}

    @Override
    public String toString() {
        return String.format("VisualLine{text='%s', start=%d, hasNewline=%b}",
                text, startPosition, hasNewlineChar);
    }

    public static void main(String[] args) {
        VisualLine vl = new VisualLine("Hello World", 2, true);
        System.out.println(vl);


    }
}



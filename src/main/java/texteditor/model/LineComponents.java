package texteditor.model;

public class LineComponents {
    public final String content;
    public final boolean hasNewLineCharacter;

    public LineComponents(String content) {
        this.content = content;
        this.hasNewLineCharacter = hasNewLineCharacter();
    }

    public boolean hasNewLineCharacter() {
        return content.endsWith("\n");
    }
}

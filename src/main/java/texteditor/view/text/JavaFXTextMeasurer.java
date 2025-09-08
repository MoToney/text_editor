package texteditor.view.text;

import javafx.scene.text.Font;
import javafx.scene.text.Text;

public class JavaFXTextMeasurer implements TextMeasurer {
    private final Font font;
    private final Text textNode;
    private final double lineHeight;
    private final double baselineOffset;

    public JavaFXTextMeasurer(Font font) {
        this.font = font;
        this.textNode = new Text();
        this.textNode.setFont(font);

        double spacing = textNode.getLineSpacing();
        this.lineHeight = spacing > 0 ? spacing : textNode.getBoundsInLocal().getHeight() * 1.2;
        this.baselineOffset = textNode.getBaselineOffset();
    }

    @Override
    public double measureWidth(String text) {
        if (text == null || text.isEmpty()) return 0.0;
        textNode.setText(text);
        return textNode.getBoundsInLocal().getWidth();
    }

    @Override
    public double getLineHeight() {
        return lineHeight;
    }

    @Override
    public double getBaselineOffset() {
        return baselineOffset;
    }

    public Font getFont() {
        return font;
    }
}

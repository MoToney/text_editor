package texteditor.view.text;

import javafx.scene.text.Font;

public interface TextMeasurer {

    double measureWidth(String text);

    double getLineHeight();

    double getBaselineOffset();

    Font getFont();
}

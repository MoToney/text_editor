package texteditor.view.text;

public interface TextMeasurer {

    double measureWidth(String text);

    double getLineHeight();

    double getBaselineOffset();
}

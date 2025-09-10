package texteditor.view.caret;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import texteditor.model.Caret;
import texteditor.model.PieceTable;
import texteditor.view.layout.VisualLine;
import texteditor.view.text.TextMeasurer;

import java.util.Arrays;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class CaretControllerTest {

    private PieceTable document;
    private TextMeasurer measurer;
    private Caret caret;
    private CaretController controller;

    private final double paddingHorizontal = 10.0;
    private final double paddingTop = 25.0;

    private List<VisualLine> visualLines;

    @BeforeEach
    void setUp() {
        document = new PieceTable(
                "Ends at 11\n" +
                        "This should start at 26 the length is harder to know because this sentence is longer"
        );

        // deterministic fake measurer
        measurer = new FakeTextMeasurer(7.0, 3.0, 10.0);

        caret = new Caret(document);
        controller = new CaretController(document, measurer, caret, paddingHorizontal, paddingTop);

        // Fake lines, simplified but realistic logic for start/end/newline
        visualLines = Arrays.asList(
                new TestVisualLine(0, 11, true, "Ends at 11\n"),
                new TestVisualLine(11, 27, false, "This should star"),
                new TestVisualLine(27, 43, false, "t at 26 the leng"),
                new TestVisualLine(43, 59, false, "th is harder to "),
                new TestVisualLine(59, 75, false, "know because thi"),
                new TestVisualLine(75, 91, false, "s sentence is lo"),
                new TestVisualLine(91, 95, false, "nger")
        );
    }

    // --- Basic caret movement ---
    @Test
    void moveLeft_decrementsPosition_and_setsAffinityRight() {
        caret.setPosition(5);
        controller.moveLeft();
        assertEquals(4, caret.getPosition());
        assertEquals(Caret.Affinity.RIGHT, caret.getAffinity());
    }

    @Test
    void moveLeft_doesNothing_atZero() {
        caret.setPosition(0);
        caret.setAffinity(Caret.Affinity.LEFT);
        controller.moveLeft();
        assertEquals(0, caret.getPosition());
        assertEquals(Caret.Affinity.LEFT, caret.getAffinity());
    }

    @Test
    void moveRight_incrementsPosition_and_setsAffinityRight() {
        caret.setPosition(5);
        controller.moveRight();
        assertEquals(6, caret.getPosition());
        assertEquals(Caret.Affinity.RIGHT, caret.getAffinity());
    }

    @Test
    void moveRight_doesNothing_atDocumentEnd() {
        caret.setPosition(document.getLength());
        controller.moveRight();
        assertEquals(document.getLength(), caret.getPosition());
    }

    // --- Line navigation ---
    @Test
    void moveToLineStart_setsPositionToLineStart_andAffinityRight() {
        caret.setPosition(15); // somewhere in line 2
        controller.moveToLineStart(visualLines);
        assertEquals(11, caret.getPosition()); // line 1 start
        assertEquals(Caret.Affinity.RIGHT, caret.getAffinity());
    }

    @Test
    void moveToLineEnd_setsPositionCorrectly_basedOnNewline() {
        caret.setPosition(5); // line 0
        controller.moveToLineEnd(visualLines);
        assertEquals(10, caret.getPosition()); // last char before \n

        caret.setPosition(12); // line 1
        controller.moveToLineEnd(visualLines);
        assertEquals(27, caret.getPosition()); // endPosition
        assertEquals(Caret.Affinity.LEFT, caret.getAffinity());
    }

    @Test
    void moveUpOrDown_movesToCorrectColumn_whenTargetLineLongEnough() {
        caret.setPosition(2); // line 0, column 2
        controller.moveUpOrDown(visualLines, 1); // move down to line 1
        assertEquals(13, caret.getPosition()); // line1 start + 2
    }

    @Test
    void moveUpOrDown_handlesShorterTargetLine_withNewline() {
        caret.setPosition(10); // line 0, last char before \n
        controller.moveUpOrDown(visualLines, 1); // move to line 1 (longer, non-newline)
        assertEquals(21, caret.getPosition()); // start of line 1 + column
    }

    @Test
    void updateCursorLocation_setsXandY_correctly() {
        caret.setPosition(2); // line 0, column 2
        controller.updateCursorLocation(visualLines);

        double expectedX = paddingHorizontal + measurer.measureWidth("En");
        double expectedY = paddingTop + measurer.getBaselineOffset() + 0 * measurer.getLineHeight();

        assertEquals(expectedX, controller.getCursorX(), 1e-9);
        assertEquals(expectedY, controller.getCursorY(), 1e-9);
    }

    @Test
    void updateCursorLocation_setsXToPadding_whenColumnZero() {
        caret.setPosition(11); // line 1 start
        controller.updateCursorLocation(visualLines);

        assertEquals(paddingHorizontal, controller.getCursorX(), 1e-9);
    }

    @Test
    void findVisualColumnIndex_returnsPositionMinusStart() {
        caret.setPosition(15); // line 1
        VisualLine line = visualLines.get(1);
        assertEquals(4, controller.findVisualColumnIndex(caret.getPosition(), line));
    }

    @Test
    void moveToClickPosition_placesCaretAtStartOfFirstLine_whenClickNearLeft() {
        // line 0 starts at position 0: "Ends at 11\n"
        double clickX = paddingHorizontal + 1; // very close to left edge
        double clickY = paddingTop + 5;        // first line (row 0)

        controller.moveToClickPosition(clickX, clickY, visualLines);

        assertEquals(0, caret.getPosition());
    }

    @Test
    void moveToClickPosition_placesCaretInMiddleOfLine_whenClickInsideText() {
        // Suppose we click after the "E" in "Ends at 11"
        double charWidth = measurer.measureWidth("E");
        double clickX = paddingHorizontal + charWidth + 1;
        double clickY = paddingTop + 5;

        controller.moveToClickPosition(clickX, clickY, visualLines);

        assertEquals(1, caret.getPosition()); // caret after 'E'
    }

    @Test
    void moveToClickPosition_clampsToEndOfLine_whenClickPastRightEdge() {
        VisualLine firstLine = visualLines.get(0);
        double lineWidth = measurer.measureWidth(firstLine.text());
        double clickX = paddingHorizontal + lineWidth + 50; // well beyond end
        double clickY = paddingTop + 5;

        controller.moveToClickPosition(clickX, clickY, visualLines);

        assertEquals(firstLine.startPosition() + firstLine.text().length() - 1, caret.getPosition());
    }

    @Test
    void moveToClickPosition_selectsCorrectLine_whenClickingOnSecondLine() {
        double clickX = paddingHorizontal + 1;
        double clickY = paddingTop + measurer.getLineHeight() + 5; // second line (row 1)

        controller.moveToClickPosition(clickX, clickY, visualLines);

        assertEquals(visualLines.get(1).startPosition(), caret.getPosition());
    }


    /* ---- Helpers ---- */
    private static class FakeTextMeasurer implements TextMeasurer {
        private final double charWidth, baselineOffset, lineHeight;

        FakeTextMeasurer(double charWidth, double baselineOffset, double lineHeight) {
            this.charWidth = charWidth;
            this.baselineOffset = baselineOffset;
            this.lineHeight = lineHeight;
        }

        @Override
        public double measureWidth(String text) {
            return (text == null || text.isEmpty()) ? 0 : text.length() * charWidth;
        }

        @Override
        public double getBaselineOffset() { return baselineOffset; }

        @Override
        public double getLineHeight() { return lineHeight; }

        @Override
        public javafx.scene.text.Font getFont() { return null; }
    }

    private static class TestVisualLine extends VisualLine {
        private final int start, end;
        private final boolean newline;
        private final String text;

        TestVisualLine(int start, int end, boolean newline, String text) {
            super();
            this.start = start;
            this.end = end;
            this.newline = newline;
            this.text = text;
        }

        @Override public int startPosition() { return start; }
        @Override public int endPosition() { return end; }
        @Override public int length() { return end - start; }
        @Override public boolean hasNewlineChar() { return newline; }
        @Override public String text() { return text; }
    }
}

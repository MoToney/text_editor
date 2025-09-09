package texteditor.view;

import javafx.animation.KeyFrame;
import javafx.animation.Timeline;
import javafx.scene.canvas.Canvas;
import javafx.scene.canvas.GraphicsContext;
import javafx.util.Duration;
import texteditor.model.PieceTable;
import texteditor.view.caret.CaretController;
import texteditor.view.layout.LayoutEngine;
import texteditor.view.layout.VisualLine;

import java.util.List;

public class EditorCanvas extends Canvas {
    private final PieceTable document;
    private final LayoutEngine layoutEngine;
    private final CaretController caretController;
    private final CanvasRenderer renderer;

    private List<VisualLine> visualLines;

    private final double paddingHorizontal;
    private final double paddingTop;


    private boolean isCursorVisible = true;
    private final Timeline cursorBlinkTimeline;

    public EditorCanvas(PieceTable document, LayoutEngine layoutEngine, CaretController caretController,
                        CanvasRenderer renderer, double paddingHorizontal, double paddingTop) {
        super(250, 300);

        this.document = document;
        this.layoutEngine = layoutEngine;
        this.caretController = caretController;
        this.renderer = renderer;

        this.paddingHorizontal = paddingHorizontal;
        this.paddingTop = paddingTop;


        this.cursorBlinkTimeline = createCursorBlinkTimeline();
        setupFocusHandling();

        this.visualLines = recalculateLayout();
        draw();
    }

    public void draw() {
        GraphicsContext gc = this.getGraphicsContext2D();
        gc.clearRect(0, 0, getWidth(), getHeight());

        visualLines = recalculateLayout();

        renderer.drawDocumentLines(gc, visualLines);
        caretController.updateCursorLocation(visualLines);
        renderer.drawCaret(gc, caretController.getCursorX(), caretController.getCursorY(), isCursorVisible);
    }

    public List<VisualLine> recalculateLayout() {
        double availableWidth = getWidth() - (paddingHorizontal * 2);
        var layoutResult = layoutEngine.calculateLayout(document, availableWidth);
        return layoutResult.getVisualLines();
    }

    public void moveLeft() {
        caretController.moveLeft();
        resetCursorBlink();
        draw();
    }

    public void moveRight() {
        caretController.moveRight();
        resetCursorBlink();
        draw();
    }

    public void moveEnd() {
        caretController.moveToLineEnd(visualLines);
        resetCursorBlink();
        draw();
        }

    public void moveHome() {
        caretController.moveToLineStart(visualLines);
        resetCursorBlink();
        draw();
    }

    public void moveDown() {
        caretController.moveVertical(visualLines, 1);
        resetCursorBlink();
        draw();
    }

    public void moveUp() {
        caretController.moveVertical(visualLines, -1);
        resetCursorBlink();
        draw();
    }

    private Timeline createCursorBlinkTimeline() {
        var timeline = new Timeline(
                new KeyFrame(Duration.seconds(0.5), event -> {
                    isCursorVisible = !isCursorVisible;
                    draw();
                })
        );
        timeline.setCycleCount(Timeline.INDEFINITE);
        timeline.play();
        return timeline;
    }

    public void resetCursorBlink() {
        isCursorVisible = true;
        cursorBlinkTimeline.playFromStart();
    }

    private void setupFocusHandling() {
        this.focusedProperty().addListener((observable, oldValue, newValue) -> {
            if (newValue) {
                resetCursorBlink();
                cursorBlinkTimeline.play();
            } else {
                isCursorVisible = false;
                draw();
                cursorBlinkTimeline.pause();
            }
        });
    }
}


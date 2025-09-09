package texteditor.view;

import javafx.animation.KeyFrame;
import javafx.animation.Timeline;
import javafx.scene.canvas.Canvas;
import javafx.scene.canvas.GraphicsContext;
import javafx.util.Duration;
import texteditor.model.PieceTable;
import texteditor.view.cursor.CursorManager;
import texteditor.view.layout.TextLayoutEngine;
import texteditor.view.layout.VisualLine;
import java.util.List;

public class EditorCanvas extends Canvas {
    private final PieceTable document;
    private final TextLayoutEngine layoutEngine;
    private final CursorManager cursorManager;
    private final EditorRenderer renderer;

    private  List<VisualLine> visualLines;

    private final double paddingHorizontal;
    private final double paddingTop;


    private boolean isCursorVisible = true;
    private final Timeline cursorBlinkTimeline;

    public EditorCanvas(PieceTable document, TextLayoutEngine layoutEngine, CursorManager cursorManager,
                        EditorRenderer renderer, double paddingHorizontal, double paddingTop) {
        super(250, 300);

        this.document = document;
        this.layoutEngine = layoutEngine;
        this.cursorManager = cursorManager;
        this.renderer = renderer;

        this.paddingHorizontal = paddingHorizontal;
        this.paddingTop = paddingTop;

        this.cursorBlinkTimeline = createCursorBlinkTimeline();
        setupFocusHandling();
    }

    public void draw() {
        GraphicsContext gc = this.getGraphicsContext2D();
        gc.clearRect(0, 0, getWidth(), getHeight());

        double availableWidth = getWidth() - paddingHorizontal - paddingHorizontal;
        var layoutResult = layoutEngine.calculateLayout(document, availableWidth);
        visualLines = layoutResult.getVisualLines();

        renderer.renderDocument(gc, visualLines);
        cursorManager.updateCursorLocation(visualLines);
        renderer.renderCursor(gc, cursorManager.getCursorX(), cursorManager.getCursorY(), isCursorVisible);
    }

    public void moveLeft() {
        cursorManager.moveLeft();
        resetCursorBlink();
        draw();
    }

    public void moveRight() {
        cursorManager.moveRight();
        resetCursorBlink();
        draw();
    }

    public void moveEnd() {
        cursorManager.moveToLineEnd(visualLines);
        resetCursorBlink();
        draw();
        }

    public void moveHome() {
        cursorManager.moveToLineStart(visualLines);
        resetCursorBlink();
        draw();
    }

    public void moveDown() {
        cursorManager.moveVertical(visualLines, 1);
        resetCursorBlink();
        draw();
    }

    public void moveUp() {
        cursorManager.moveVertical(visualLines, -1);
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

    private void resetCursorBlink() {
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


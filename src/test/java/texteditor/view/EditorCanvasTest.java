/*package texteditor.view;
import javafx.application.Platform;
import javafx.scene.Scene;
import javafx.scene.layout.StackPane;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import texteditor.controller.EditorController;
import texteditor.model.CursorModel;
import texteditor.model.PieceTable;

import static org.junit.jupiter.api.Assertions.*;
import javafx.embed.swing.JFXPanel;

import java.util.concurrent.CountDownLatch;

class EditorCanvasTest {

    private PieceTable document;
    private CursorModel cursor;
    private EditorCanvas canvas;


    @BeforeAll
    static void initToolkit() {
        // This call is enough to start the JavaFX runtime
        new JFXPanel();
    }

    @BeforeEach
    void setUp() throws InterruptedException {
        CountDownLatch latch = new CountDownLatch(1);

        Platform.runLater(() -> {
            document = new PieceTable("Ends at 11\nThis should start...");
            canvas = new EditorCanvas(document);
            cursor = new CursorModel(document, canvas);

            canvas.setCursor(cursor);
            canvas.calculateFontMetrics();
            canvas.draw();

            StackPane root = new StackPane(canvas);
            Scene scene = new Scene(root, 300, 300);
            new EditorController(scene, document, cursor, canvas);

            latch.countDown();
        });

        latch.await(); // wait for UI setup to complete
    }

    @Test
    void testMoveEndOnWrappedLine() {
        // 2. Define the initial state (cursor is at position 0)
        cursor.setPosition(2); // Place cursor on the first visual line ("A long...")

        // 3. Perform the action
        canvas.moveEnd();

        // 4. Assert the result
        // Assuming "A long " is the first visual line of length 7
        int expectedPosition = 7;
        assertEquals(expectedPosition, cursor.getPosition(), "Cursor should be at the end of the first visual line.");
        assertEquals(CursorModel.Affinity.LEFT, cursor.getAffinity(), "Affinity should be LEFT at the end of a wrapped line.");
    }
}
*/
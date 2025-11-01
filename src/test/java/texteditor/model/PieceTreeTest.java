package texteditor.model;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class PieceTreeTest {

    private PieceTable table;

    @BeforeEach
    void setup() {
        table = new PieceTable("Hello\nWorld");
    }

    @Test
    void testInitialText() {
        assertEquals("Hello\nWorld", table.getText());
        assertEquals(11, table.length());
    }

    @Test
    void testInsertAtStart() {
        table.insert(0, "Say: ");
        assertEquals("Say: Hello\nWorld", table.getText());
    }

    @Test
    void testInsertAtMiddle() {
        table.insert(5, ", dear");
        assertEquals("Hello, dear\nWorld", table.getText());
    }

    @Test
    void testInsertAtEnd() {
        table.insert(table.length(), "!");
        assertEquals("Hello\nWorld!", table.getText());
    }

    @Test
    void testRemoveSingleCharacter() {
        table.remove(5, 1); // remove newline
        assertEquals("HelloWorld", table.getText());
    }

    @Test
    void testRemoveMiddleSubstring() {
        table.remove(2, 5); // remove "llo\nW"
        assertEquals("Heorld", table.getText());
    }

    @Test
    void testRemoveFullText() {
        table.remove(0, table.length());
        assertEquals("", table.getText());
    }

    @Test
    void testInsertEmptyString() {
        table.insert(3, "");
        assertEquals("Hello\nWorld", table.getText());
    }

    @Test
    void testRemoveNegativeLength() {
        table.remove(0, -5);
        assertEquals("Hello\nWorld", table.getText());
    }

    @Test
    void testRemoveBeyondLength() {
        table.remove(8, 10);
        assertEquals("Hello\nWo", table.getText());
    }

}
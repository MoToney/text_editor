package texteditor.model;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

public class
PieceTableTest {

    @Test
    public void constructorAndBasicGetters() {
        PieceTable pt = new PieceTable("Hello");
        assertEquals("Hello", pt.getText());
        assertEquals(5, pt.getLength());
        assertEquals(1, pt.getLineCount());
        assertEquals("Hello", pt.getLine(0));
        assertTrue(pt.isLastLine(0));
        assertEquals(5, pt.getLineLength(0));
    }

    @Test
    public void insertAtBeginning() {
        PieceTable pt = new PieceTable("world");
        pt.insert(0, "Hello ");
        assertEquals("Hello world", pt.getText());
        assertEquals(11, pt.getLength());
    }

    @Test
    public void insertInMiddleSplitsPiece() {
        PieceTable pt = new PieceTable("abcde");
        pt.insert(2, "X");
        assertEquals("abXcde", pt.getText());
        assertEquals(6, pt.getLength());
    }

    @Test
    public void insertAtEndAppends() {
        PieceTable pt = new PieceTable("abc");
        pt.insert(3, "def");
        assertEquals("abcdef", pt.getText());
        assertEquals(6, pt.getLength());
    }

    @Test
    public void insertIntoEmptyDocument() {
        PieceTable pt = new PieceTable("");
        pt.insert(0, "abc");
        assertEquals("abc", pt.getText());
        assertEquals(3, pt.getLength());
        assertEquals(1, pt.getLineCount());
    }

    @Test
    public void removeWithinSinglePiece() {
        PieceTable pt = new PieceTable("abcdef");
        pt.remove(2, 2); // remove "cd"
        assertEquals("abef", pt.getText());
        assertEquals(4, pt.getLength());
    }

    @Test
    public void removeSpanningPieces() {
        // Build a document that will have multiple pieces after an insert
        PieceTable pt = new PieceTable("HelloWorld");
        pt.insert(5, "12345"); // Hello12345World
        // Remove 10 chars starting at index 3 (spans original + inserted content)
        pt.remove(3, 10);
        // Expected result calculated manually: "Hel" + remaining "ld" -> "Helld"
        assertEquals("Helld", pt.getText());
        assertEquals(5, pt.getLength());
    }

    @Test
    public void removeOutOfBoundsDoesNothing() {
        PieceTable pt = new PieceTable("hello");
        // attempt to remove starting beyond document length -> should be no-op
        pt.remove(10, 2);
        assertEquals("hello", pt.getText());
        assertEquals(5, pt.getLength());
    }

    @Test
    public void lineCacheAndGetLineBehavior() {
        PieceTable pt = new PieceTable("line1\nline2\nlast");
        assertEquals(3, pt.getLineCount());
        // lines that end in a newline include the newline character in this implementation
        assertEquals("line1\n", pt.getLine(0));
        assertEquals("line2\n", pt.getLine(1));
        assertEquals("last", pt.getLine(2));
        assertTrue(pt.isLastLine(2));
        assertEquals(6, pt.getLineLength(0)); // "line1\n" length
        assertEquals(4, pt.getLineLength(2)); // "last" length
    }

    @Test
    public void insertingNewlineUpdatesLineCount() {
        PieceTable pt = new PieceTable("abc\ndef");
        assertEquals(2, pt.getLineCount());
        // insert a newline in the first line (split the first line)
        pt.insert(1, "\n"); // "a\nbc\ndef"
        assertEquals(3, pt.getLineCount());
        assertEquals("a\n", pt.getLine(0));
        assertEquals("bc\n", pt.getLine(1));
        assertEquals("def", pt.getLine(2));
    }

    @Test
    public void getAllDocumentTextMatchesGetTextInitially() {
        PieceTable pt = new PieceTable("Hello World");
        assertEquals("Hello World", pt.getText());
    }

    @Test
    public void getTextMatchesAfterInsert() {
        PieceTable pt = new PieceTable("Hello");
        pt.insert(5, " World");
        assertEquals("Hello World", pt.getText());
    }

    @Test
    public void getTextMatchesAfterRemove() {
        PieceTable pt = new PieceTable("abcdef");
        pt.remove(2, 3); // remove cde
        assertEquals("abf", pt.getText());
    }

    @Test
    public void get() {
        PieceTable pt = new PieceTable("");
        assertEquals("", pt.getText());
    }

    @Test
    public void insertAndRemoveSequence() {
        PieceTable pt = new PieceTable("HelloWorld");

        // Insert text in the middle
        pt.insert(5, "12345"); // "Hello12345World"
        assertEquals("Hello12345World", pt.getText());
        assertEquals(15, pt.getLength());

        // Remove part of the inserted text + some original text
        pt.remove(3, 7); // removes "lo12345" -> "HelWorld"
        assertEquals("HelWorld", pt.getText());
        assertEquals(8, pt.getLength());

        // Insert again at the beginning
        pt.insert(0, "Start-"); // "Start-HelWorld"
        assertEquals("Start-HelWorld", pt.getText());
        assertEquals(14, pt.getLength());

        // Remove at the end
        pt.remove(pt.getLength() - 3, 3); // removes "rld" -> "Start-HelWo"
        assertEquals("Start-HelWo", pt.getText());
        assertEquals(11, pt.getLength());
    }

    @Test
    public void insertAndRemoveWithNewlines() {
        PieceTable pt = new PieceTable("Hello\nWorld"); // length = 11

        // Insert text with newlines in the middle
        pt.insert(5, "\n123\n"); // "Hello\n123\n\nWorld"
        // Length calculation: "Hello" (5) + "\n123\n" (5) + "\nWorld" (6) = 16
        assertEquals("Hello\n123\n\nWorld", pt.getText());
        assertEquals(16, pt.getLength());
        assertEquals(4, pt.getLineCount()); // lines: "Hello\n", "123\n", "\n", "World"

        // Remove across multiple lines
        pt.remove(4, 6); // removes "o\n123\n" -> "Hell\nWorld"
        // Length calculation: "Hell" (4) + "\nWorld" (6) = 10
        assertEquals("Hell\nWorld", pt.getText());
        assertEquals(10, pt.getLength());
        assertEquals(2, pt.getLineCount()); // lines: "Hell\n", "World"

        // Insert newline at the beginning
        pt.insert(0, "\nStart\n"); // "\nStart\nHell\nWorld"
        // Length calculation: "\nStart\n" (7) + "Hell\nWorld" (10) = 17
        assertEquals("\nStart\nHell\nWorld", pt.getText());
        assertEquals(17, pt.getLength());
        assertEquals(4, pt.getLineCount()); // lines: "\n", "Start\n", "Hell\n", "World"

        // Remove newline and text at the end
        pt.remove(pt.getLength() - 5, 5); // removes "World" -> "\nStart\nHell\n"
        // Length calculation: "\nStart\nHell\n" = 12
        assertEquals("\nStart\nHell\n", pt.getText());
        assertEquals(12, pt.getLength());
        assertEquals(3, pt.getLineCount()); // lines: "\n", "Start\n", "Hell\
    }
}
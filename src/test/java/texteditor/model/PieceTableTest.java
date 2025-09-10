package texteditor.model;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

public class PieceTableTest {

    @Test
    public void constructorAndBasicGetters() {
        PieceTable pt = new PieceTable("Hello");
        assertEquals("Hello", pt.getAllDocumentText());
        assertEquals(5, pt.getDocumentLength());
        assertEquals(1, pt.getLineCount());
        assertEquals("Hello", pt.getLine(0));
        assertTrue(pt.isLastLine(0));
        assertEquals(5, pt.getLineLength(0));
    }

    @Test
    public void insertAtBeginning() {
        PieceTable pt = new PieceTable("world");
        pt.insertText(0, "Hello ");
        assertEquals("Hello world", pt.getAllDocumentText());
        assertEquals(11, pt.getDocumentLength());
    }

    @Test
    public void insertInMiddleSplitsPiece() {
        PieceTable pt = new PieceTable("abcde");
        pt.insertText(2, "X");
        assertEquals("abXcde", pt.getAllDocumentText());
        assertEquals(6, pt.getDocumentLength());
    }

    @Test
    public void insertAtEndAppends() {
        PieceTable pt = new PieceTable("abc");
        pt.insertText(3, "def");
        assertEquals("abcdef", pt.getAllDocumentText());
        assertEquals(6, pt.getDocumentLength());
    }

    @Test
    public void insertIntoEmptyDocument() {
        PieceTable pt = new PieceTable("");
        pt.insertText(0, "abc");
        assertEquals("abc", pt.getAllDocumentText());
        assertEquals(3, pt.getDocumentLength());
        assertEquals(1, pt.getLineCount());
    }

    @Test
    public void removeWithinSinglePiece() {
        PieceTable pt = new PieceTable("abcdef");
        pt.removeText(2, 2); // remove "cd"
        assertEquals("abef", pt.getAllDocumentText());
        assertEquals(4, pt.getDocumentLength());
    }

    @Test
    public void removeSpanningPieces() {
        // Build a document that will have multiple pieces after an insert
        PieceTable pt = new PieceTable("HelloWorld");
        pt.insertText(5, "12345"); // Hello12345World
        // Remove 10 chars starting at index 3 (spans original + inserted content)
        pt.removeText(3, 10);
        // Expected result calculated manually: "Hel" + remaining "ld" -> "Helld"
        assertEquals("Helld", pt.getAllDocumentText());
        assertEquals(5, pt.getDocumentLength());
    }

    @Test
    public void removeOutOfBoundsDoesNothing() {
        PieceTable pt = new PieceTable("hello");
        // attempt to remove starting beyond document length -> should be no-op
        pt.removeText(10, 2);
        assertEquals("hello", pt.getAllDocumentText());
        assertEquals(5, pt.getDocumentLength());
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
        pt.insertText(1, "\n"); // "a\nbc\ndef"
        assertEquals(3, pt.getLineCount());
        assertEquals("a\n", pt.getLine(0));
        assertEquals("bc\n", pt.getLine(1));
        assertEquals("def", pt.getLine(2));
    }

    @Test
    public void getAllDocumentTextMatchesGetTextInitially() {
        PieceTable pt = new PieceTable("Hello World");
        assertEquals("Hello World", pt.getAllDocumentText());
    }

    @Test
    public void getAllDocumentTextMatchesAfterInsert() {
        PieceTable pt = new PieceTable("Hello");
        pt.insertText(5, " World");
        assertEquals("Hello World", pt.getAllDocumentText());
    }

    @Test
    public void getAllDocumentTextMatchesAfterRemove() {
        PieceTable pt = new PieceTable("abcdef");
        pt.removeText(2, 3); // remove cde
        assertEquals("abf", pt.getAllDocumentText());
    }

    @Test
    public void getAllDocumentTextHandlesEmptyDocument() {
        PieceTable pt = new PieceTable("");
        assertEquals("", pt.getAllDocumentText());
    }

}

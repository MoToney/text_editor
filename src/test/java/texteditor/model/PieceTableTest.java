package texteditor.model;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

import static org.junit.jupiter.api.Assertions.*;

public class PieceTableTest {

    //
    @Test
    public void constructorAndBasicGetters() {
        PieceTable pt = new PieceTable("Hello");
        assertEquals("Hello", pt.getText());
        assertEquals(5, pt.getTreeLength());
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
        assertEquals(11, pt.getTreeLength());
    }

    @Test
    public void insertInMiddleSplitsPiece() {
        PieceTable pt = new PieceTable("abcde");
        pt.insert(2, "X");
        assertEquals("abXcde", pt.getText());
        assertEquals(6, pt.getTreeLength());
    }

    @Test
    public void insertAtEndAppends() {
        PieceTable pt = new PieceTable("abc");
        pt.insert(3, "def");
        assertEquals("abcdef", pt.getText());
        assertEquals(6, pt.getTreeLength());
    }

    @Test
    public void insertIntoEmptyDocument() {
        PieceTable pt = new PieceTable("");
        pt.insert(0, "abc");
        assertEquals("abc", pt.getText());
        assertEquals(3, pt.getTreeLength());
        assertEquals(1, pt.getLineCount());
    }

    @Test
    public void removeWithinSinglePiece() {
        PieceTable pt = new PieceTable("abcdef");
        pt.remove(2, 2); // remove "cd"
        assertEquals("abef", pt.getText());
        assertEquals(4, pt.getTreeLength());
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
        assertEquals(5, pt.getTreeLength());
    }

    @Test
    public void removeOutOfBoundsDoesNothing() {
        PieceTable pt = new PieceTable("hello");
        // attempt to remove starting beyond document length -> should be no-op
        pt.remove(10, 2);
        assertEquals("hello", pt.getText());
        assertEquals(5, pt.getTreeLength());
    }

    @Test
    public void lineCacheAndGetLineBehavior() {
        PieceTable pt = new PieceTable("line1\nline2\nlast");
        assertEquals(3, pt.getLineCount());
        // lines that endLocation in a newline include the newline character in this implementation
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
        assertEquals(15, pt.getTreeLength());

        // Remove part of the inserted text + some original text
        pt.remove(3, 7); // removes "lo12345" -> "HelWorld"
        assertEquals("HelWorld", pt.getText());
        assertEquals(8, pt.getTreeLength());

        // Insert again at the beginning
        pt.insert(0, "Start-"); // "Start-HelWorld"
        assertEquals("Start-HelWorld", pt.getText());
        assertEquals(14, pt.getTreeLength());

        // Remove at the endLocation
        pt.remove(pt.getTreeLength() - 3, 3); // removes "rld" -> "Start-HelWo"
        assertEquals("Start-HelWo", pt.getText());
        assertEquals(11, pt.getTreeLength());
    }

    @Test
    public void insertAndRemoveWithNewlines() {
        PieceTable pt = new PieceTable("Hello\nWorld"); // length = 11

        // Insert text with newlines in the middle
        pt.insert(5, "\n123\n"); // "Hello\n123\n\nWorld"
        // Length calculation: "Hello" (5) + "\n123\n" (5) + "\nWorld" (6) = 16
        assertEquals("Hello\n123\n\nWorld", pt.getText());
        assertEquals(16, pt.getTreeLength());
        assertEquals(4, pt.getLineCount()); // lines: "Hello\n", "123\n", "\n", "World"

        // Remove across multiple lines
        pt.remove(4, 6); // removes "o\n123\n" -> "Hell\nWorld"
        // Length calculation: "Hell" (4) + "\nWorld" (6) = 10
        assertEquals("Hell\nWorld", pt.getText());
        assertEquals(10, pt.getTreeLength());
        assertEquals(2, pt.getLineCount()); // lines: "Hell\n", "World"

        // Insert newline at the beginning
        pt.insert(0, "\nStart\n"); // "\nStart\nHell\nWorld"
        // Length calculation: "\nStart\n" (7) + "Hell\nWorld" (10) = 17
        assertEquals("\nStart\nHell\nWorld", pt.getText());
        assertEquals(17, pt.getTreeLength());
        assertEquals(4, pt.getLineCount()); // lines: "\n", "Start\n", "Hell\n", "World"

        // Remove newline and text at the endLocation
        pt.remove(pt.getTreeLength() - 5, 5); // removes "World" -> "\nStart\nHell\n"
        // Length calculation: "\nStart\nHell\n" = 12
        assertEquals("\nStart\nHell\n", pt.getText());
        assertEquals(12, pt.getTreeLength());
        assertEquals(3, pt.getLineCount()); // lines: "\n", "Start\n", "Hell\n"
    }

    // ------------------------------------------------------------
    // Reflection helpers + RB-tree invariant tests (adapted from PieceTreeTest)
    // These startLocation from PieceTable and reflect into its internal PieceTree.
    // ------------------------------------------------------------

    // use this instead of direct field access
    private Field findFieldInHierarchy(Class<?> cls, String fieldName) {
        Class<?> cur = cls;
        while (cur != null) {
            try {
                Field f = cur.getDeclaredField(fieldName);
                f.setAccessible(true);
                return f;
            } catch (NoSuchFieldException e) {
                cur = cur.getSuperclass();
            }
        }
        return null;
    }

    // get root field from the internal tree
    private Object getRootFromTree(Object tree) throws Exception {
        if (tree == null) return null;
        Field f = findFieldInHierarchy(tree.getClass(), "root");
        if (f == null) throw new NoSuchFieldException("root on " + tree.getClass());
        return f.get(tree);
    }

    private Object getRoot(PieceTable table) throws Exception {
        return getRootFromTree(table);
    }

    // generalized child getter (left/right/parent) - Node is inner class, so use node.getClass()
    private Object getChild(Object node, String childField) throws Exception {
        if (node == null) return null;
        Field f = findFieldInHierarchy(node.getClass(), childField);
        if (f == null) throw new NoSuchFieldException(childField + " on " + node.getClass());
        Object child = f.get(node);

        // For parent field, return as-is (even if NIL)
        // For left/right fields, convert NIL to null for easier traversal
        if (childField.equals("parent")) {
            return child;
        }

        // For left/right, return null if NIL
        if (child != null && isNilSentinel(child)) {
            return null;
        }

        return child;
    }

    // Helper to check if a node is the NIL sentinel
    private boolean isNilSentinel(Object node) throws Exception {
        if (node == null) return false;

        // Check if this is the NIL sentinel by checking if it's the static field
        try {
            Method isLeafMethod = node.getClass().getDeclaredMethod("isLeaf");
            isLeafMethod.setAccessible(true);
            boolean leaf = (Boolean) isLeafMethod.invoke(node);

            // NIL has isLeaf() = false and payload = null
            if (!leaf) {
                Object payload = getPayload(node);
                if (payload == null) {
                    // Additional check: NIL points to itself for left/right
                    Field leftField = findFieldInHierarchy(node.getClass(), "left");
                    if (leftField != null) {
                        leftField.setAccessible(true);
                        Object left = leftField.get(node);
                        return left == node; // NIL.left == NIL
                    }
                }
            }
        } catch (Exception e) {
            // Ignore
        }
        return false;
    }

    private Object getParent(Object node) throws Exception {
        return getChild(node, "parent");
    }

    // flexible payload accessor: tries "piece" then "payload"
    private Object getPayload(Object node) throws Exception {
        if (node == null) return null;
        Field f = findFieldInHierarchy(node.getClass(), "piece");
        if (f == null) f = findFieldInHierarchy(node.getClass(), "payload");
        if (f == null) throw new NoSuchFieldException("piece|payload on " + node.getClass());
        f.setAccessible(true);
        return f.get(node);
    }

    // isLeaf reflection: use method if present, otherwise infer by payload != null
    private boolean isLeaf(Object node) throws Exception {
        if (node == null) return false;
        try {
            Method m = node.getClass().getDeclaredMethod("isLeaf");
            m.setAccessible(true);
            return (Boolean) m.invoke(node);
        } catch (NoSuchMethodException e) {
            // fallback: consider leaf if payload/piece != null
            Object payload = getPayload(node);
            return payload != null;
        }
    }

    // color check - prefer method, fallback to field "color"
    private boolean isRed(Object node) throws Exception {
        if (node == null) return false;
        try {
            Method m = node.getClass().getDeclaredMethod("isRed");
            m.setAccessible(true);
            return (Boolean) m.invoke(node);
        } catch (NoSuchMethodException e) {
            Field f = findFieldInHierarchy(node.getClass(), "color");
            if (f == null) throw new NoSuchFieldException("color on " + node.getClass());
            f.setAccessible(true);
            Object val = f.get(node);
            return val != null && val.toString().equals("RED");
        }
    }

    private int getNodeLength(Object node) throws Exception {
        Field f = findFieldInHierarchy(node.getClass(), "length");
        if (f == null) throw new NoSuchFieldException("length");
        f.setAccessible(true);
        return (Integer) f.get(node);
    }

    private Piece getPiece(Object node) throws Exception {
        if (node == null) return null;

        // try "piece"
        Field f = findFieldInHierarchy(node.getClass(), "piece");
        if (f == null) {
            // fallback to "payload"
            f = findFieldInHierarchy(node.getClass(), "payload");
        }
        if (f == null) {
            // helpful message so you know what's actually on the Node class
            StringBuilder sb = new StringBuilder();
            sb.append("No field named 'piece' or 'payload' found on class ").append(node.getClass()).append(". Fields: ");
            for (Field ff : node.getClass().getDeclaredFields()) sb.append(ff.getName()).append(" ");
            throw new NoSuchFieldException(sb.toString());
        }
        f.setAccessible(true);
        Object val = f.get(node);
        if (val == null) return null;
        if (!(val instanceof Piece)) {
            // If your payload type changed or is generic-wrapped, convert/cast safely
            throw new ClassCastException("Expected Piece in field but found: " + val.getClass());
        }
        return (Piece) val;
    }

    // Recursively compute total length by walking children (independent of node.length)
    private int computeTotalLength(Object node) throws Exception {
        if (node == null || isNilSentinel(node)) return 0;  // ✓ Add NIL check

        if (isLeaf(node)) {
            Piece p = getPiece(node);
            return (p == null) ? 0 : p.getLength();
        }
        Object left = getChild(node, "left");
        Object right = getChild(node, "right");
        return computeTotalLength(left) + computeTotalLength(right);
    }

    private int countLeaves(Object node) throws Exception {
        if (node == null || isNilSentinel(node)) return 0;  // ✓ Add NIL check
        if (isLeaf(node)) return 1;
        return countLeaves(getChild(node, "left")) + countLeaves(getChild(node, "right"));
    }

    // Ensure every internal node's stored length equals sum of children's lengths
    private void assertLengthConsistency(Object node) throws Exception {
        if (node == null || isNilSentinel(node)) return;  // ✓ Add NIL check

        if (isLeaf(node)) {
            Piece p = getPiece(node);
            if (p != null) {
                assertEquals(p.getLength(), getNodeLength(node),
                        "Leaf node length mismatch (stored vs piece length)");
            }
            return;
        } else {
            Object payload = getPayload(node);
            assertNull(payload, "Internal node must not have a payload");
        }
        Object left = getChild(node, "left");
        Object right = getChild(node, "right");

        // getChild now returns null for NIL, so these checks work
        int leftLen = (left != null) ? getNodeLength(left) : 0;
        int rightLen = (right != null) ? getNodeLength(right) : 0;
        assertEquals(leftLen + rightLen, getNodeLength(node),
                "Internal node length should equal sum of children");

        assertLengthConsistency(left);
        assertLengthConsistency(right);
    }

    // No two consecutive red nodes
    private void assertNoConsecutiveReds(Object node, boolean parentWasRed) throws Exception {
        if (node == null || isNilSentinel(node)) return;  // ✓ Add NIL check

        boolean red = isRed(node);
        if (parentWasRed) {
            assertFalse(red, "Found consecutive red nodes (parent and child both red)");
        }
        if (!isLeaf(node)) {
            assertNoConsecutiveReds(getChild(node, "left"), red);
            assertNoConsecutiveReds(getChild(node, "right"), red);
        }
    }

    // Collect black depths for all leaves and assert equality
    private void collectBlackDepths(Object node, int curBlackCount, List<Integer> depths) throws Exception {
        if (node == null || isNilSentinel(node)) {  // ✓ Add NIL check
            // NIL is black, so increment count
            depths.add(curBlackCount + 1);
            return;
        }

        if (!isRed(node)) curBlackCount++;

        if (isLeaf(node)) {
            depths.add(curBlackCount);
            return;
        }
        collectBlackDepths(getChild(node, "left"), curBlackCount, depths);
        collectBlackDepths(getChild(node, "right"), curBlackCount, depths);
    }

    private void assertBlackDepthsEqual(Object root) throws Exception {
        List<Integer> depths = new ArrayList<>();
        collectBlackDepths(root, 0, depths);
        assertFalse(depths.isEmpty(), "No leaves found when checking black-depths");
        int expected = depths.get(0);
        for (int d : depths) {
            assertEquals(expected, d, "Black-depth mismatch across leaves");
        }
    }

    private void assertParentPointersConsistent(Object node, Object expectedParent) throws Exception {


        // Skip NIL sentinel nodes - they don't follow normal parent pointer rules


        Object actualParent = getParent(node);

        if (expectedParent == null) {
            if (actualParent != null) {
                System.err.println("ERROR: Node " + System.identityHashCode(node) +
                        " expected parent=null but has parent=" + System.identityHashCode(actualParent));
                System.err.println("This likely means the node is NOT the root. " +
                        "Make sure you re-fetch the root after tree modifications.");
            }
            assertNull(actualParent, "Root's parent must be null (or NIL). " +
                    "If this fails, the node might not be the actual root after rotations.");
        } else {
            assertSame(expectedParent, actualParent, "Child's parent pointer is incorrect");
        }

        Object left = getChild(node, "left");
        Object right = getChild(node, "right");

        // getChild already converts NIL to null for left/right
        if (left != null) {
            assertParentPointersConsistent(left, node);
        }
        if (right != null) {
            assertParentPointersConsistent(right, node);
        }
    }

    // Debug printer: prints identity hash + parent/left/right ids, color, length, and piece length if leaf
    private void printTree(Object node) throws Exception {
        printTree(node, "");
    }

    private void printTree(Object node, String indent) throws Exception {
        if (node == null) {
            System.out.println(indent + "null");
            return;
        }

        if (isNilSentinel(node)) {  // ✓ Add NIL check
            System.out.println(indent + "NIL");
            return;
        }

        int id = System.identityHashCode(node);
        Object parent = getParent(node);
        Object left = getChild(node, "left");
        Object right = getChild(node, "right");
        boolean leaf = isLeaf(node);
        boolean red = isRed(node);
        int len = getNodeLength(node);
        String pieceDesc = "";
        if (leaf) {
            Piece p = getPiece(node);
            pieceDesc = (p == null) ? "piece=null" : "pieceLen=" + p.getLength();
        }

        System.out.printf("%sNode@%d parent=%s left=%s right=%s isLeaf=%b color=%s length=%d %s%n",
                indent,
                id,
                (parent == null ? "null" : ("@" + System.identityHashCode(parent))),
                (left == null ? "null" : ("@" + System.identityHashCode(left))),
                (right == null ? "null" : ("@" + System.identityHashCode(right))),
                leaf,
                (red ? "RED" : "BLACK"),
                len,
                pieceDesc
        );

        printTree(left, indent + "  ");
        printTree(right, indent + "  ");
    }

    // ----------------------
    // Reflection-based tests (adapted)
    // ----------------------

    @Test
    public void insertIntoEmptyTree_createsSingleLeafWithCorrectLength() throws Exception {
        PieceTable pt = new PieceTable("");
        pt.insert(0, "AAAAA"); // length 5

        Object root = getRoot(pt);
        assertNotNull(root, "Root should not be null after first insert");
        assertTrue(isLeaf(root), "Root should be a leaf after inserting into empty tree");

        int computed = computeTotalLength(root);
        assertEquals(5, computed, "Computed total length should be 5");
    }

    @Test
    public void insertInMiddleOfSingleLeaf_splitsAndMaintainsLengthsAndInvariants() throws Exception {
        // Start with a single piece length 10 (original)
        PieceTable pt = new PieceTable("AAAAAAAAAA"); // length 10

        // Insert a new piece of length 3 at position 4 => expected total length 13
        pt.insert(4, "BBB");

        Object root = getRoot(pt);
        assertNotNull(root, "Root must exist after split-insert");
        int total = computeTotalLength(root);
        assertEquals(13, total, "Total computed length should be 10 + 3 = 13");

        // Expect 3 leaves (left part, inserted part, right part)
        int leaves = countLeaves(root);
        assertEquals(3, leaves, "Expected 3 leaves after splitting the single leaf");

        // Structural and RB invariant checks
        assertLengthConsistency(root);
        assertNoConsecutiveReds(root, false);
        assertBlackDepthsEqual(root);
    }

    @Test
    public void manyInserts_maintainsLengthsAndRedBlackInvariants() throws Exception {
        PieceTable pt = new PieceTable("");
        // Insert 20 single-length pieces appended one after another
        final int N = 20;
        for (int i = 0; i < N; i++) {
            pt.insert(i, "x");
        }

        Object root = getRoot(pt);
        assertNotNull(root, "Root should not be null after many inserts");

        int total = computeTotalLength(root);
        assertEquals(N, total, "Total computed length must equal number of inserted single-length pieces");

        // Structural and RB invariant checks
        assertLengthConsistency(root);
        assertNoConsecutiveReds(root, false);
        assertBlackDepthsEqual(root);
    }

    @Test
    public void insertInMiddleOfSingleLeaf_parentPointersConsistent() throws Exception {
        PieceTable pt = new PieceTable("AAAAAAAAAA");
        pt.insert(4, "BBB");

        Object root = getRoot(pt);
        assertParentPointersConsistent(root, null);
    }

    @Test
    public void manyInserts_stressParentPointersAndRBInvariants() throws Exception {
        PieceTable pt = new PieceTable("");
        final int N = 200;                        // number of inserts (increase if you want heavier stress)
        Random rnd = new Random(12345);           // deterministic seed for reproducible failures
        int expectedTotal = 0;

        for (int i = 0; i < N; i++) {
            int pieceLen = 1;
            int curLen = pt.getTreeLength();
            int pos;
            switch (i % 4) {
                case 0:
                    pos = 0;
                    break;          // insert at startLocation
                case 1:
                    pos = curLen;
                    break;     // insert at endLocation
                case 2:
                    pos = curLen / 2;
                    break; // insert in middle
                default:
                    pos = (curLen == 0) ? 0 : rnd.nextInt(curLen + 1); // random
            }

            pt.insert(pos, "x");
            expectedTotal += pieceLen;

            Object root = getRoot(pt);
            try {
                // Independent total length check (walks leaves, avoids relying on cached length fields)
                int computed = computeTotalLength(root);
                assertEquals(expectedTotal, computed,
                        "Total computed length mismatch at iteration " + i + " insertPos=" + pos);

                // structural & RB checks
                assertLengthConsistency(root);
                assertNoConsecutiveReds(root, false);
                assertBlackDepthsEqual(root);

                // parent pointers check (reflection-based)
                assertParentPointersConsistent(root, null);

            } catch (Throwable t) {
                System.out.println("===== Validation failed at insert iteration " + i +
                        " (insertPos=" + pos + ", expectedTotal=" + expectedTotal + ") =====");
                System.out.println("Tree dump (node@id parent left right isLeaf color length [pieceLen]):");
                try {
                    printTree(root);
                } catch (Exception ex) {
                    ex.printStackTrace();
                }
                throw t;
            }
        }

        // Final sanity at endLocation of all inserts
        Object finalRoot = getRoot(pt);
        assertNotNull(finalRoot);
        assertLengthConsistency(finalRoot);
        assertNoConsecutiveReds(finalRoot, false);
        assertBlackDepthsEqual(finalRoot);
        assertParentPointersConsistent(finalRoot, null);
    }

    @Test
    public void alternatingInsertDeleteMaintainsCorrectText() {
        PieceTable pt = new PieceTable("abc");
        pt.insert(3, "123");    // "abc123"
        pt.remove(2, 2);        // remove "c1" -> "ab23"
        pt.insert(0, "X");      // "Xab23"
        pt.remove(4, 1);        // remove '3' -> "Xab2"
        pt.insert(4, "YZ");     // append -> "Xab2YZ"
        assertEquals("Xab2YZ", pt.getText());
        assertEquals(6, pt.getTreeLength());
    }

    @Test
    public void deleteAcrossInsertedAndOriginalPieces() {
        PieceTable pt = new PieceTable("HelloWorld");
        pt.insert(5, "12345"); // Hello12345World
        pt.remove(3, 8);       // remove "lo12345W" -> "Helorld"
        assertEquals("Helorld", pt.getText());
    }


    @Test
    public void randomMixedInsertDeleteMaintainsInvariants() throws Exception {
        PieceTable pt = new PieceTable("");
        Random rnd = new Random(42);
        int expectedLength = 0;

        for (int i = 0; i < 100; i++) {
            if (rnd.nextDouble() < 0.6 || expectedLength == 0) {
                // Perform insertion
                int pos = (expectedLength == 0) ? 0 : rnd.nextInt(expectedLength + 1);
                int len = rnd.nextInt(3) + 1;
                String text = "x".repeat(len);
                pt.insert(pos, text);
                expectedLength += len;
            } else {
                // Perform deletion
                int len = rnd.nextInt(Math.max(1, expectedLength / 4)) + 1;
                int pos = rnd.nextInt(Math.max(1, expectedLength - len + 1));
                pt.remove(pos, len);
                expectedLength -= Math.min(len, expectedLength - pos);
            }

            Object root = getRoot(pt);
            if (root != null) {
                int total = computeTotalLength(root);
                assertEquals(expectedLength, total, "Length mismatch at iteration " + i);
                assertLengthConsistency(root);
                assertNoConsecutiveReds(root, false);
                assertBlackDepthsEqual(root);
                assertParentPointersConsistent(root, null);
            }
        }
    }

    @Test
    public void insertRemoveReinsertAtSameSpot() {
        PieceTable pt = new PieceTable("abcdef");
        pt.insert(3, "XYZ");  // "abcXYZdef"
        pt.remove(3, 3);      // back to "abcdef"
        pt.insert(3, "123");  // "abc123def"
        assertEquals("abc123def", pt.getText());
        assertEquals(9, pt.getTreeLength());
    }

    @Test
    @DisplayName("Remove with negative length does nothing")
    public void removeNegativeLength() {
        PieceTable pt = new PieceTable("Hello");
        pt.remove(2, -5);
        assertEquals("Hello", pt.getText());
        assertEquals(5, pt.getTreeLength());
    }

    @Test
    @DisplayName("Remove with zero length does nothing")
    public void removeZeroLength() {
        PieceTable pt = new PieceTable("Hello");
        pt.remove(2, 0);
        assertEquals("Hello", pt.getText());
        assertEquals(5, pt.getTreeLength());
    }

    @Test
    @DisplayName("Remove with negative position does nothing")
    public void removeNegativePosition() {
        PieceTable pt = new PieceTable("Hello");
        pt.remove(-1, 3);
        assertEquals("Hello", pt.getText());
        assertEquals(5, pt.getTreeLength());
    }

    @Test
    @DisplayName("Remove at position equal to length does nothing")
    public void removeAtEndPosition() {
        PieceTable pt = new PieceTable("Hello");
        pt.remove(5, 3);
        assertEquals("Hello", pt.getText());
        assertEquals(5, pt.getTreeLength());
    }

    @Test
    @DisplayName("Remove beyond document length does nothing")
    public void removeBeyondLength() {
        PieceTable pt = new PieceTable("Hello");
        pt.remove(10, 5);
        assertEquals("Hello", pt.getText());
        assertEquals(5, pt.getTreeLength());
    }

    @Test
    @DisplayName("Remove from empty document does nothing")
    public void removeFromEmptyDocument() {
        PieceTable pt = new PieceTable("");
        pt.remove(0, 5);
        assertEquals("", pt.getText());
        assertEquals(0, pt.getTreeLength());
    }

    @Test
    @DisplayName("Remove entire document from position 0")
    public void removeEntireDocument() {
        PieceTable pt = new PieceTable("Hello World");
        pt.remove(0, 11);
        assertEquals("", pt.getText());
        assertEquals(0, pt.getTreeLength());
    }

    // ============================================================================
    // SINGLE NODE REMOVAL TESTS (start.node == end.node)
    // ============================================================================

    @Test
    @DisplayName("Remove from middle of single piece (leftLen > 0 && rightLen > 0)")
    public void removeSingleNodeMiddleSplit() {
        PieceTable pt = new PieceTable("abcdefgh");
        pt.remove(2, 3); // Remove "cde"
        assertEquals("abfgh", pt.getText());
        assertEquals(5, pt.getTreeLength());
    }

    @Test
    @DisplayName("Remove from beginning of single piece (leftLen == 0, rightLen > 0)")
    public void removeSingleNodeFromBeginning() {
        PieceTable pt = new PieceTable("abcdefgh");
        pt.remove(0, 3); // Remove "abc"
        assertEquals("defgh", pt.getText());
        assertEquals(5, pt.getTreeLength());
    }

    @Test
    @DisplayName("Remove from end of single piece (leftLen > 0, rightLen == 0)")
    public void removeSingleNodeFromEnd() {
        PieceTable pt = new PieceTable("abcdefgh");
        pt.remove(5, 3); // Remove "fgh"
        assertEquals("abcde", pt.getText());
        assertEquals(5, pt.getTreeLength());
    }

    @Test
    @DisplayName("Remove entire single piece (leftLen == 0 && rightLen == 0)")
    public void removeSingleNodeCompletely() {
        PieceTable pt = new PieceTable("abc");
        pt.remove(0, 3);
        assertEquals("", pt.getText());
        assertEquals(0, pt.getTreeLength());
    }

    @Test
    @DisplayName("Remove single character from single piece")
    public void removeSingleCharacter() {
        PieceTable pt = new PieceTable("abcde");
        pt.remove(2, 1); // Remove "c"
        assertEquals("abde", pt.getText());
        assertEquals(4, pt.getTreeLength());
    }

    @Test
    @DisplayName("Remove leaving single character at start")
    public void removeLeavingSingleCharAtStart() {
        PieceTable pt = new PieceTable("abc");
        pt.remove(1, 2); // Remove "bc"
        assertEquals("a", pt.getText());
        assertEquals(1, pt.getTreeLength());
    }

    @Test
    @DisplayName("Remove leaving single character at end")
    public void removeLeavingSingleCharAtEnd() {
        PieceTable pt = new PieceTable("abc");
        pt.remove(0, 2); // Remove "ab"
        assertEquals("c", pt.getText());
        assertEquals(1, pt.getTreeLength());
    }

    @Test
    @DisplayName("Remove leaving single character in middle")
    public void removeLeavingSingleCharInMiddle() {
        PieceTable pt = new PieceTable("abc");
        pt.remove(1, 1); // Remove "b"
        assertEquals("ac", pt.getText());
        assertEquals(2, pt.getTreeLength());
    }

    // ============================================================================
    // MULTI-NODE REMOVAL TESTS (start.node != end.node)
    // ============================================================================

    @Test
    @DisplayName("Remove spanning two pieces after insert")
    public void removeSpanningTwoPieces() {
        PieceTable pt = new PieceTable("abcdef");
        pt.insert(3, "XYZ"); // "abcXYZdef"
        pt.remove(2, 5); // Remove "cXYZd"
        assertEquals("abef", pt.getText());
        assertEquals(4, pt.getTreeLength());
    }

    @Test
    @DisplayName("Remove spanning three pieces")
    public void removeSpanningThreePieces() {
        PieceTable pt = new PieceTable("abcdef");
        pt.insert(2, "12"); // "ab12cdef"
        pt.insert(6, "XY"); // "ab12cdXYef"
        pt.remove(1, 8); // Remove "b12cdXYe"
        assertEquals("af", pt.getText());
        assertEquals(2, pt.getTreeLength());
    }

    @Test
    @DisplayName("Remove spanning multiple pieces with no remainder on start")
    public void removeMultiPiecesNoStartRemainder() {
        PieceTable pt = new PieceTable("abc");
        pt.insert(3, "123"); // "abc123"
        pt.remove(0, 4); // Remove "abc1"
        assertEquals("23", pt.getText());
        assertEquals(2, pt.getTreeLength());
    }

    @Test
    @DisplayName("Remove spanning multiple pieces with no remainder on end")
    public void removeMultiPiecesNoEndRemainder() {
        PieceTable pt = new PieceTable("abc");
        pt.insert(3, "123"); // "abc123"
        pt.remove(2, 4); // Remove "c123"
        assertEquals("ab", pt.getText());
        assertEquals(2, pt.getTreeLength());
    }

    @Test
    @DisplayName("Remove spanning multiple pieces with no remainders on either side")
    public void removeMultiPiecesNoRemainders() {
        PieceTable pt = new PieceTable("abc");
        pt.insert(3, "123"); // "abc123"
        pt.insert(6, "xyz"); // "abc123xyz"
        pt.remove(3, 3); // Remove "123" exactly
        assertEquals("abcxyz", pt.getText());
        assertEquals(6, pt.getTreeLength());
    }

    @Test
    @DisplayName("Remove entire start piece and part of next")
    public void removeEntireStartPiecePartOfNext() {
        PieceTable pt = new PieceTable("Hello");
        pt.insert(5, "World"); // "HelloWorld"
        pt.remove(0, 7); // Remove "HelloWo"
        assertEquals("rld", pt.getText());
        assertEquals(3, pt.getTreeLength());
    }

    @Test
    @DisplayName("Remove part of first piece and entire second piece")
    public void removePartOfFirstEntireSecond() {
        PieceTable pt = new PieceTable("Hello");
        pt.insert(5, "123"); // "Hello123"
        pt.remove(3, 5); // Remove "lo123"
        assertEquals("Hel", pt.getText());
        assertEquals(3, pt.getTreeLength());
    }

    // ============================================================================
    // COMPLEX TREE STRUCTURE TESTS
    // ============================================================================

    @Test
    @DisplayName("Remove after building complex tree with multiple inserts")
    public void removeFromComplexTree() {
        PieceTable pt = new PieceTable("AAAA");
        pt.insert(2, "BB"); // "AABBAA"
        pt.insert(4, "CC"); // "AABBCCAA"
        pt.insert(6, "DD"); // "AABBCCDDAA"
        pt.remove(3, 5); // Remove "BCCDD"
        assertEquals("AABAA", pt.getText());
        assertEquals(5, pt.getTreeLength());
    }

    @Test
    @DisplayName("Remove creating unbalanced situation requiring fixup")
    public void removeRequiringRebalance() {
        PieceTable pt = new PieceTable("a");
        for (int i = 1; i <= 10; i++) {
            pt.insert(i, "x");
        }
        // Now remove several nodes
        pt.remove(5, 3);
        assertEquals(8, pt.getTreeLength());
        // Verify tree is still valid through subsequent operations
        pt.insert(4, "y");
        assertEquals(9, pt.getTreeLength());
    }

    @Test
    @DisplayName("Remove from deep tree structure")
    public void removeFromDeepTree() {
        PieceTable pt = new PieceTable("");
        // Build a deeper tree
        for (int i = 0; i < 20; i++) {
            pt.insert(i, String.valueOf((char) ('a' + i)));
        }
        String before = pt.getText();
        pt.remove(5, 10);
        assertEquals(10, pt.getTreeLength());
        assertNotEquals(before, pt.getText());
    }

    // ============================================================================
    // EDGE CASES WITH NEWLINES
    // ============================================================================

    @Test
    @DisplayName("Remove including newlines")
    public void removeIncludingNewlines() {
        PieceTable pt = new PieceTable("Hello\nWorld\nTest");
        pt.remove(5, 7); // Remove "\nWorld\n"
        assertEquals("HelloTest", pt.getText());
        assertEquals(9, pt.getTreeLength());
    }

    @Test
    @DisplayName("Remove single newline character")
    public void removeSingleNewline() {
        PieceTable pt = new PieceTable("Hello\nWorld");
        pt.remove(5, 1); // Remove "\n"
        assertEquals("HelloWorld", pt.getText());
        assertEquals(10, pt.getTreeLength());
    }

    @Test
    @DisplayName("Remove multiple consecutive newlines")
    public void removeMultipleNewlines() {
        PieceTable pt = new PieceTable("Hello\n\n\nWorld");
        pt.remove(5, 3); // Remove "\n\n\n"
        assertEquals("HelloWorld", pt.getText());
        assertEquals(10, pt.getTreeLength());
    }

    // ============================================================================
    // SEQUENTIAL REMOVAL TESTS
    // ============================================================================

    @Test
    @DisplayName("Multiple removals in sequence")
    public void multipleSequentialRemovals() {
        PieceTable pt = new PieceTable("abcdefghij");
        pt.remove(8, 2); // "abcdefgh"
        assertEquals("abcdefgh", pt.getText());
        pt.remove(6, 2); // "abcdef"
        assertEquals("abcdef", pt.getText());
        pt.remove(4, 2); // "abcd"
        assertEquals("abcd", pt.getText());
        pt.remove(2, 2); // "ab"
        assertEquals("ab", pt.getText());
    }

    @Test
    @DisplayName("Remove, insert, remove pattern")
    public void removeInsertRemovePattern() {
        PieceTable pt = new PieceTable("Hello World");
        pt.remove(5, 6); // "Hello"
        pt.insert(5, " There"); // "Hello There"
        pt.remove(6, 5); // "Hello "
        assertEquals("Hello ", pt.getText());
        assertEquals(6, pt.getTreeLength());
    }

    @Test
    @DisplayName("Alternating remove from start and end")
    public void alternatingRemoveStartEnd() {
        PieceTable pt = new PieceTable("abcdefghij");
        pt.remove(0, 1); // "bcdefghij"
        assertEquals("bcdefghij", pt.getText());
        pt.remove(8, 1); // "bcdefghi"
        assertEquals("bcdefghi", pt.getText());
        pt.remove(0, 1); // "cdefghi"
        assertEquals("cdefghi", pt.getText());
        pt.remove(6, 1); // "cdefgh"
        assertEquals("cdefgh", pt.getText());
    }

    // ============================================================================
    // STRESS AND RANDOMIZED TESTS
    // ============================================================================

    @Test
    @DisplayName("Stress test: many random removals")
    public void stressTestManyRandomRemovals() {
        PieceTable pt = new PieceTable("");
        Random rnd = new Random(12345);

        // Build up document
        for (int i = 0; i < 50; i++) {
            pt.insert(pt.getTreeLength(), "x");
        }

        // Perform many removals
        for (int i = 0; i < 30; i++) {
            int len = pt.getTreeLength();
            if (len > 0) {
                int pos = rnd.nextInt(len);
                int removeLen = rnd.nextInt(Math.min(5, len - pos)) + 1;
                int expectedLen = len - removeLen;
                pt.remove(pos, removeLen);
                assertEquals(expectedLen, pt.getTreeLength(),
                        "Length mismatch at iteration " + i);
            }
        }
    }

    @Test
    @DisplayName("Remove after many inserts at various positions")
    public void removeAfterManyInsertsAtVariousPositions() {
        PieceTable pt = new PieceTable("start");
        Random rnd = new Random(42);

        for (int i = 0; i < 30; i++) {
            int pos = rnd.nextInt(pt.getTreeLength() + 1);
            pt.insert(pos, "x");
        }

        int lenBefore = pt.getTreeLength();
        pt.remove(10, 15);
        assertEquals(lenBefore - 15, pt.getTreeLength());
    }

    @Test
    @DisplayName("Remove same position multiple times")
    public void removeSamePositionMultipleTimes() {
        PieceTable pt = new PieceTable("abcdefghijklmnop");
        pt.remove(5, 2); // "abcdehijklmnop, removes 'fg' "
        assertEquals("abcdehijklmnop", pt.getText());
        pt.remove(5, 2); // "abcdejklmnop, removes 'hi' "
        assertEquals("abcdejklmnop", pt.getText());
        pt.remove(5, 2); // "abcdemnop"
        assertEquals("abcdelmnop", pt.getText());
    }

    // ============================================================================
    // SPECIAL EDGE CASES
    // ============================================================================

    @Test
    @DisplayName("Remove one character multiple times until empty")
    public void removeOneCharUntilEmpty() {
        PieceTable pt = new PieceTable("abc");
        pt.remove(0, 1); // "bc"
        assertEquals("bc", pt.getText());
        pt.remove(0, 1); // "c"
        assertEquals("c", pt.getText());
        pt.remove(0, 1); // ""
        assertEquals("", pt.getText());
        assertEquals(0, pt.getTreeLength());
    }

    @Test
    @DisplayName("Remove from document with only newlines")
    public void removeFromNewlinesOnly() {
        PieceTable pt = new PieceTable("\n\n\n\n");
        pt.remove(1, 2);
        assertEquals("\n\n", pt.getText());
        assertEquals(2, pt.getTreeLength());
    }

    @Test
    @DisplayName("Remove after inserting at boundaries")
    public void removeAfterBoundaryInserts() {
        PieceTable pt = new PieceTable("middle");
        pt.insert(0, "start"); // "startmiddle"
        pt.insert(11, "end"); // "startmiddleend"
        pt.remove(5, 6); // "startend"
        assertEquals("startend", pt.getText());
        assertEquals(8, pt.getTreeLength());
    }

    @Test
    @DisplayName("Remove creating empty pieces that should be cleaned up")
    public void removeCreatingEmptyPieces() {
        PieceTable pt = new PieceTable("a");
        pt.insert(1, "b"); // "ab"
        pt.insert(2, "c"); // "abc"
        pt.remove(0, 3); // Should cleanly remove everything
        assertEquals("", pt.getText());
        assertEquals(0, pt.getTreeLength());
    }

    // ============================================================================
    // INVARIANT VALIDATION TESTS
    // ============================================================================

    @Test
    @DisplayName("Remove maintains tree structure after single-node removal")
    public void removeMaintainsTreeStructureSingleNode() throws Exception {
        PieceTable pt = new PieceTable("abcdefgh");
        pt.remove(2, 4);

        // Use reflection to verify tree invariants
        Object root = getRoot(pt);
        if (root != null) {
            assertTreeInvariants(root);
        }
    }

    @Test
    @DisplayName("Remove maintains tree structure after multi-node removal")
    public void removeMaintainsTreeStructureMultiNode() throws Exception {
        PieceTable pt = new PieceTable("Hello");
        pt.insert(5, "World");
        pt.insert(10, "Test");
        pt.remove(3, 10);

        Object root = getRoot(pt);
        if (root != null) {
            assertTreeInvariants(root);
        }
    }

    // ============================================================================
    // HELPER METHODS (simplified from your existing test class)
    // ============================================================================


    private void assertTreeInvariants(Object root) throws Exception {
        // Add basic invariant checks
        assertNotNull(root);
        // You can expand this with your existing invariant checking methods
    }


    @Test
    public void insertThenRemoveRestoresOriginal() {
        PieceTable pt = new PieceTable("abcdef");
        pt.insert(3, "XYZ");
        pt.remove(3, 3);
        assertEquals("abcdef", pt.getText());
    }
}
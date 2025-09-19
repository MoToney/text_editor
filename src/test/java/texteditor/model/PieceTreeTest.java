package texteditor.model;

import org.junit.jupiter.api.Test;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

import static org.junit.jupiter.api.Assertions.*;

public class PieceTreeTest {

    // use this instead of PieceTree.class.getDeclaredField("root")
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

    private Object getRoot(PieceTree tree) throws Exception {
        Field f = findFieldInHierarchy(tree.getClass(), "root");
        if (f == null) throw new NoSuchFieldException("root");
        return f.get(tree);
    }

    // generalized child getter (left/right/parent) - Node class is an inner class, so use node.getClass()
    private Object getChild(Object node, String childField) throws Exception {
        if (node == null) return null;
        Field f = findFieldInHierarchy(node.getClass(), childField);
        if (f == null) throw new NoSuchFieldException(childField + " on " + node.getClass());
        return f.get(node);
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
        if (node == null) return 0;
        if (isLeaf(node)) {
            Piece p = getPiece(node);
            return (p == null) ? 0 : p.getLength();
        }
        Object left = getChild(node, "left");
        Object right = getChild(node, "right");
        return computeTotalLength(left) + computeTotalLength(right);
    }

    private int countLeaves(Object node) throws Exception {
        if (node == null) return 0;
        if (isLeaf(node)) return 1;
        return countLeaves(getChild(node, "left")) + countLeaves(getChild(node, "right"));
    }
    // Ensure every internal node's stored length equals sum of children's lengths
    private void assertLengthConsistency(Object node) throws Exception {
        if (node == null) return;
        if (isLeaf(node)) {
            Piece p = getPiece(node);
            if (p != null) {
                assertEquals(p.getLength(), getNodeLength(node),
                        "Leaf node length mismatch (stored vs piece length)");
            }
            return;
        }
        Object left = getChild(node, "left");
        Object right = getChild(node, "right");
        int leftLen = (left != null) ? getNodeLength(left) : 0;
        int rightLen = (right != null) ? getNodeLength(right) : 0;
        assertEquals(leftLen + rightLen, getNodeLength(node),
                "Internal node length should equal sum of children");
        // recurse
        assertLengthConsistency(left);
        assertLengthConsistency(right);
    }

    // No two consecutive red nodes
    private void assertNoConsecutiveReds(Object node, boolean parentWasRed) throws Exception {
        if (node == null) return;
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
        if (node == null) return;
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

    // Reflection-aware parent-pointer assertion (replace any old direct-access version)
    private void assertParentPointersConsistent(Object node, Object expectedParent) throws Exception {
        if (node == null) return;

        Object actualParent = getParent(node);

        if (expectedParent == null) {
            assertNull(actualParent, "Root's parent must be null");
        } else {
            assertSame(expectedParent, actualParent, "Child's parent pointer is incorrect");
        }

        Object left = getChild(node, "left");
        Object right = getChild(node, "right");

        assertParentPointersConsistent(left, node);
        assertParentPointersConsistent(right, node);
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
                (parent == null ? "null" : ("@"+System.identityHashCode(parent))),
                (left == null ? "null" : ("@"+System.identityHashCode(left))),
                (right == null ? "null" : ("@"+System.identityHashCode(right))),
                leaf,
                (red ? "RED" : "BLACK"),
                len,
                pieceDesc
        );

        printTree(left, indent + "  ");
        printTree(right, indent + "  ");
    }

    // ----------------------
    // Tests
    // ----------------------

    @Test
    public void insertIntoEmptyTree_createsSingleLeafWithCorrectLength() throws Exception {
        PieceTree tree = new PieceTree();
        Piece p = new Piece(Piece.BufferType.ADD, 0, 5);
        tree.insert(0, p);

        Object root = getRoot(tree);
        assertNotNull(root, "Root should not be null after first insert");
        assertTrue(isLeaf(root), "Root should be a leaf after inserting into empty tree");

    }
    @Test
    public void insertInMiddleOfSingleLeaf_splitsAndMaintainsLengthsAndInvariants() throws Exception {
        PieceTree tree = new PieceTree();
        // Start with a single piece length 10
        Piece original = new Piece(Piece.BufferType.ADD, 0, 10);
        tree.insert(0, original);

        // Insert a new piece of length 3 at position 4 => expected total length 13
        Piece inserted = new Piece(Piece.BufferType.ADD, 0, 3);
        tree.insert(4, inserted);

        Object root = getRoot(tree);
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
        PieceTree tree = new PieceTree();
        // Insert 20 single-length pieces appended one after another
        final int N = 20;
        for (int i = 0; i < N; i++) {
            tree.insert(i, new Piece(Piece.BufferType.ADD, 0, 1));
        }

        Object root = getRoot(tree);
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
        PieceTree tree = new PieceTree();
        Piece original = new Piece(Piece.BufferType.ADD, 0, 10);
        tree.insert(0, original);

        Piece inserted = new Piece(Piece.BufferType.ADD, 0, 3);
        tree.insert(4, inserted);

        Object root = getRoot(tree);
        assertParentPointersConsistent(root, null);
    }

    @Test
    public void manyInserts_stressParentPointersAndRBInvariants() throws Exception {
        PieceTree tree = new PieceTree();
        final int N = 200;                        // number of inserts (increase if you want heavier stress)
        Random rnd = new Random(12345);           // deterministic seed for reproducible failures
        int expectedTotal = 0;

        for (int i = 0; i < N; i++) {
            int pieceLen = 1;
            int curLen = tree.treeLength();          // public length() on PieceTree
            int pos;
            switch (i % 4) {
                case 0: pos = 0; break;          // insert at start
                case 1: pos = curLen; break;     // insert at end
                case 2: pos = curLen / 2; break; // insert in middle
                default: pos = (curLen == 0) ? 0 : rnd.nextInt(curLen + 1); // random
            }

            tree.insert(pos, new Piece(Piece.BufferType.ADD, 0, pieceLen));
            expectedTotal += pieceLen;

            Object root = getRoot(tree);
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
                try { printTree(root); } catch (Exception ex) { ex.printStackTrace(); }
                throw t;
            }
        }

        // Final sanity at end of all inserts
        Object finalRoot = getRoot(tree);
        assertNotNull(finalRoot);
        assertLengthConsistency(finalRoot);
        assertNoConsecutiveReds(finalRoot, false);
        assertBlackDepthsEqual(finalRoot);
        assertParentPointersConsistent(finalRoot, null);
    }



}

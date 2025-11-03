package texteditor.model;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class PieceTreeTest {

    private PieceTree tree;

    private static class SimpleBuffer extends Buffer {
        private final char[] data;

        SimpleBuffer(char[] data) {
            super(Type.ADD); // or ORIGINAL, as needed
            this.data = data;
        }

        @Override
        public int length() {
            return data.length;
        }

        @Override
        public char charAt(int index) {
            return data[index];
        }

        @Override
        public String substring(int start, int end) {
            return new String(data, start, end - start);
        }

        @Override
        public String getText() {
            return new String(data);
        }

        @Override
        public String toString() {
            return getText();
        }
    }

    @BeforeEach
    void setup() {
        tree = new PieceTree();
    }

    private Piece makePiece(String s) {
        Buffer buf = new SimpleBuffer(s.toCharArray());
        return new Piece(buf, 0, buf.length());
    }
    // ========================
    // Node creation & leaf
    // ========================
    @Test
    void testCreateNodeIsLeaf() {
        Piece p = makePiece("abc");
        PieceTree.PieceNode node = tree.createNode(p);

        assertNotNull(node);
        assertTrue(tree.isLeaf(node));
        assertEquals(3, node.length());
        assertEquals(0, node.lftSize);
        assertEquals(0, node.lftLineCount);
    }

    // ========================
    // Sentinel node
    // ========================

    @Test
    void testSentinelNodeProperties() {
        PieceTree.PieceNode sentinel = tree.createSentinel();

        assertNotNull(sentinel);
        assertEquals(sentinel, sentinel.left);
        assertEquals(sentinel, sentinel.right);
        assertEquals(sentinel, sentinel.parent);
        assertEquals(PieceTree.Color.BLACK, sentinel.color);
        assertEquals(0, sentinel.lftSize);
        assertEquals(0, sentinel.lftLineCount);
    }

    // ========================
    // Length and line computation
    // ========================

    @Test
    void testLengthAndLinesSingleNode() {
        PieceTree.PieceNode node = tree.createNode(makePiece("abc\nxyz"));
        tree.setRoot(node);
        tree.updateTreeMetrics();

        assertEquals(7, tree.length()); // total chars
        assertEquals(1, node.totalLines()); // node.lines() returns line count
        assertEquals(1, tree.lines); // totalLines should match
    }

    @Test
    void testCalcLengthMultipleNodes() {
        PieceTree.PieceNode left = tree.createNode(makePiece("ab\nc"));
        PieceTree.PieceNode right = tree.createNode(makePiece("de"));
        PieceTree.PieceNode root = tree.createNode(makePiece("f"));
        root.left = left;
        root.right = right;
        left.parent = root;
        right.parent = root;
        tree.setRoot(root);

        tree.updateTreeMetrics();
        assertEquals(7, tree.length());
        assertEquals(1, tree.lines); // only one newline total
    }

    // ========================
    // Insert helpers
    // ========================

    @Test
    void testInsertAtEmptyTree() {
        Piece p = makePiece("abc");
        tree.insertHelper(0, p);

        assertEquals(3, tree.length());
        assertTrue(tree.isLeaf(tree.getRoot()));
    }

    @Test
    void testInsertAtEnd() {
        tree.insertHelper(0, makePiece("abc"));
        tree.insertHelper(3, makePiece("XYZ"));

        assertEquals(6, tree.root.size());
    }

    // ========================
    // Remove helpers
    // ========================

    @Test
    void testRemoveNodeTail() {
        PieceTree.PieceNode node = tree.createNode(makePiece("abcdef"));
        tree.setRoot(node);

        tree.deleteNodeTail(node, 3);
        assertEquals(3, node.length());
        assertEquals("abc", node.payload.getText());
    }

    @Test
    void testRemoveNodeHead() {
        PieceTree.PieceNode node = tree.createNode(makePiece("abcdef"));
        tree.setRoot(node);

        tree.deleteNodeHead(node, 2);
        assertEquals(4, node.length());
        assertEquals("cdef", node.payload.getText());
    }

    @Test
    void testDeleteNodeCharsInRange() {
        PieceTree.PieceNode node = tree.createNode(makePiece("abcdef"));
        tree.setRoot(node);

        tree.deleteNodeCharsInRange(node, 1, 4); // remove "bcd"
        assertEquals(3, tree.length());
    }

    // ========================
    // Node replacement
    // ========================

    @Test
    void testReplaceNode() {
        PieceTree.PieceNode oldNode = tree.createNode(makePiece("abc"));
        PieceTree.PieceNode newNode = tree.createNode(makePiece("XYZ"));
        tree.setRoot(oldNode);

        tree.replaceNode(newNode, oldNode, tree.NIL);

        assertEquals(tree.getRoot(), newNode);
    }

    // ========================
    // getNodeLocation & traversal
    // ========================

    @Test
    void testGetNodeLocationBasic() {
        tree.insertHelper(0, makePiece("abc"));
        tree.insertHelper(3, makePiece("XYZ"));

        PieceTree.NodeLocation loc = tree.getNodeLocation(4);
        assertEquals('Y', loc.node().payload.getText().charAt(loc.localOffset()));
    }

    @Test
    void testGetTextTraversal() {
        tree.insertHelper(0, makePiece("abc"));
        tree.insertHelper(3, makePiece("def"));
        tree.insertHelper(6, makePiece("ghi"));


        assertEquals(9, tree.length());
    }

    // ========================
    // Edge cases
    // ========================

    @Test
    void testInsertBeyondLengthThrows() {
        tree.insertHelper(0, makePiece("abc"));
        Exception e = assertThrows(IllegalArgumentException.class, () -> tree.insertHelper(5, makePiece("X")));
        assertTrue(e.getMessage().contains("Illegal global offset"));
    }
}

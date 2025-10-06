package texteditor.model;

import java.util.Objects;
import java.util.Optional;
import java.util.OptionalInt;

public class PieceTree extends RBTree<PieceTree.PieceNode, Piece> {

    protected static class PieceNode extends Node<PieceNode, Piece> {
        private int newlineCharCount;
        PieceNode(Piece payload) { super(payload); }
        PieceNode(PieceNode left, PieceNode right) {
            super(left, right);
            if (left != null) left.parent = this;
            if (right != null) right.parent = this;
        }

        @Override
        public boolean isLeaf() { return payload != null; }

        public int getNewlineCharCount() { return newlineCharCount; }
    }

    public PieceTree(Piece initial) {
        if (initial != null) {
            this.root = createLeafNode(initial);
            this.root.color = Color.BLACK;
        }
    }
    public PieceTree() {this(null);}

    @Override
    protected int length() { return (root != null) ? root.length : 0; }

    @Override
    protected void setRoot(PieceNode node) {
        this.root = node;
        this.root.color = Color.BLACK;
        recompute(this.root);
    }

    @Override
    protected PieceNode getRoot() { return this.root; }

    @Override
    protected PieceNode createLeafNode(Piece payload) {
        PieceNode node = new PieceNode(payload);
        recompute(node);
        return node;
    }

    @Override
    protected PieceNode createInternalNode(PieceNode left, PieceNode right) {
        PieceNode node = new PieceNode(left, right);
        recompute(node);
        return node;
    }

    @Override
    protected void recompute(PieceNode node) {
        if (node == null) return;
        if (node.isLeaf()) {
            node.length = (node.payload != null) ? node.payload.getLength() : 0;
            node.newlineCharCount = ( node.payload != null) ? node.payload.getLineCount() : 0;
        } else {
            node.length =
                    (node.left  != null ? node.left.length : 0) +
                            (node.right != null ? node.right.length : 0);
            node.newlineCharCount =
                    (node.left  != null ? node.left.newlineCharCount : 0) +
                            (node.right != null ? node.right.newlineCharCount : 0);
        }
    }

    @Override
    protected void addSiblingNode(PieceNode oldNode, PieceNode newNode, boolean newOnLeft) {
        PieceNode grandparent = oldNode.parent; // this was originally the parent of the node that needs a sibling

        newNode.color = Color.RED;
        Color newParentColor = oldNode.color;
        oldNode.color = Color.RED;


        PieceNode newParent = newOnLeft ? createInternalNode(newNode, oldNode) : createInternalNode(oldNode, newNode);
        newParent.color = newParentColor;
        replaceChild(grandparent, oldNode, newParent);
    }

    public void splitLeafNode(PieceNode oldNode, PieceNode newNode, int offset) {
        PieceNode grandparent = oldNode.parent;

        Piece oldPiece = oldNode.payload;
        int oldLength = oldPiece.getLength();

        Piece leftPiece = new Piece(oldPiece.getBuffer(), oldPiece.getStart(), offset);
        Piece rightPiece = new Piece(oldPiece.getBuffer(), oldPiece.getStart() + offset, oldLength - offset);

        PieceNode leftNode = createLeafNode(leftPiece);
        PieceNode rightNode = createLeafNode(rightPiece);

        leftNode.color = Color.RED;
        rightNode.color = Color.RED;
        newNode.color = Color.RED;

        PieceNode rightSubTree = createInternalNode(newNode, rightNode);
        rightSubTree.color = Color.RED;

        PieceNode newParent = createInternalNode(leftNode, rightSubTree);
        newParent.color = oldNode.color;
        replaceChild(grandparent, oldNode, newParent);
    }

    record NodeOffset(PieceNode node, int offset) {}

    Optional<NodeOffset> findNodeAndOffset(int position) {
        if (root == null) return Optional.empty();
        PieceNode node = root;

        position = Math.min(length(), Math.max(position, 0));

        while (!Objects.requireNonNull(node).isLeaf()) {
            int leftLen = (node.left != null) ? node.left.length : 0;
            if (position < leftLen) {
                node = node.left;
            } else {
                position -= leftLen;
                node = node.right;
            }
        }
        return Optional.of(new NodeOffset(node, position));
    }

    public OptionalInt findNthNewlinePos(int n) {
        if (root == null || n < 0 || n >= root.newlineCharCount) return OptionalInt.empty();

        PieceNode node = root;
        int acc = 0;

        while (!node.isLeaf()) {
            int leftCnt = (node.left != null) ? node.left.newlineCharCount : 0;
            int leftLen = (node.left != null) ? node.left.length : 0;
            if (n < leftCnt) {
                node = node.left;
            } else {
                n -= leftCnt;
                acc += leftLen;
                node = node.right;
            }
        }

        PieceNode cur = node;
        while (cur != null) {
            for (int i = 0; i < cur.length; i++) {
                if (cur.payload.getChar(i) == '\n') {
                    if (n == 0) return OptionalInt.of(acc + i);
                    n--;
                }
            }
            acc += cur.length;
            cur = nextLeaf(cur);
        }
        return OptionalInt.empty();
    }

    record NodeRange(NodeOffset start, NodeOffset end) {}

    NodeRange findNodeAndRange(int position, int removeLength) {
        if (root == null) throw new IllegalStateException("tree is empty");
        if (removeLength <= 0) throw new IllegalArgumentException("remove length must be positive");

        if (position < 0 || position >= length()) throw new IllegalArgumentException("position must be between 0 and " + (length() - 1));

        int endPos = Math.min(length(), position + removeLength);

        NodeOffset start = findNodeAndOffset(position).orElse(null);
        NodeOffset end = findNodeAndOffset(endPos).orElse(null);

        return (start != null && end != null) ? new NodeRange(start, end) : null;
    }

    PieceNode removeBetweenLeaves(PieceNode startLeaf, PieceNode endLeaf) {
        if (startLeaf == null || endLeaf == null) throw new IllegalArgumentException("Illegal remove between leaves");

        PieceNode curLeaf = nextLeaf(startLeaf);
        if (curLeaf == null || curLeaf == endLeaf) return startLeaf;

        while (curLeaf != null && curLeaf != endLeaf) {
            PieceNode nextLeaf = nextLeaf(curLeaf);

            PieceNode removedLeaf = curLeaf;
            PieceNode parent = removedLeaf.parent;
            replaceChild(parent, removedLeaf, null);

            if (removedLeaf.isBlack()) {
                PieceNode problemNode = findNodeForFixup(removedLeaf);
                if (problemNode != null) removeFixup(problemNode);
            }
            curLeaf = nextLeaf;
        }
        return startLeaf;
    }

    private PieceNode leftmost(PieceNode node) {
        PieceNode cur = node;
        while (cur != null && !cur.isLeaf()) {
            cur = cur.left;
        }
        return cur;
    }

    public PieceNode nextLeaf(PieceNode leaf) {
        if (leaf == null) return null;

        PieceNode p = leaf.parent;
        if (p == null) return null;

        // get the right sibling of the current left node
        if (p.left == leaf) return leftmost(p.right);

        // traverse up the tree until the next leaf node is found
        PieceNode cur = leaf;
        PieceNode anc = p;
        while (anc != null && anc.right == cur) {
            cur = anc;
            anc = anc.parent;
        }
        if (anc == null) return null;
        return leftmost(anc.right);
    }

    public boolean isValidRedBlack() {
        if (root != null && root.isRed()) return false;  // Root must be black
        return checkRedBlackProperties(root) != -1;
    }

    private int checkRedBlackProperties(Node node)  {
        if (node == null) return 0;  // Null nodes are black

        // Check for red-red violations
        if (node.isRed()) {
            if ((node.left != null && node.left.isRed()) ||
                    (node.right != null && node.right.isRed())) {
                return -1;  // Red-red violation
            }
        }

        int leftHeight = checkRedBlackProperties(node.left);
        int rightHeight = checkRedBlackProperties(node.right);

        if (leftHeight == -1 || rightHeight == -1 || leftHeight != rightHeight) {
            return -1;  // Black height violation
        }

        return leftHeight + (node.isBlack() ? 1 : 0);
    }

}
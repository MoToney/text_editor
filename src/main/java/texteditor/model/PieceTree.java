package texteditor.model;

import java.util.Objects;
import java.util.Optional;

public class PieceTree extends RBTree<Piece> {

    protected static class PieceNode extends Node<Piece> {
        int newlineCount;
        PieceNode(Piece payload) { super(payload); }
        PieceNode(Node<Piece> left, Node<Piece> right) {super(left, right);}
    }

    public PieceTree(Piece initial) {
        if (initial != null) {
            this.root = createLeafNode(initial);
            this.root.color = Color.BLACK;
        }
    }
    public PieceTree() {this(null);}

    protected void setRoot(Node<Piece> node) {
        this.root = node;
        this.root.color = Color.BLACK;
        this.recompute(this.root);
        return;
    }

    @Override
    protected Node<Piece> createLeafNode(Piece payload) {
        PieceNode node = new PieceNode(payload);
        recompute(node);
        return node;
    }

    @Override
    protected Node<Piece> createInternalNode(Node<Piece> left, Node<Piece> right) {
        PieceNode node = new PieceNode(left, right);
        recompute(node);
        return node;
    }

    @Override
    protected void recompute(Node<Piece> node) {
        if (node == null) return;

        PieceNode pieceNode = (PieceNode) node;

        if (node.isLeaf()) {
            node.length = (node.payload != null) ? node.payload.getLength() : 0;
            // pieceNode.newlineCount = (node.payload != null) ? node.payload.getLineCount : 0;
        } else {
            int leftLen = (node.left != null) ? node.left.length : 0;
            int rightLen = (node.right != null) ? node.right.length : 0;
            node.length = leftLen + rightLen;
        }
    }


    void addSiblingNode(Node<Piece> oldNode, Node<Piece> newNode, boolean newOnLeft) {
        Node<Piece> grandparent = oldNode.parent; // this was originally the parent of the node that needs a sibling

        newNode.color = Color.RED;
        Color newParentColor = oldNode.color;
        oldNode.color = Color.RED;


        Node<Piece> newParent = newOnLeft ? createInternalNode(newNode, oldNode) : createInternalNode(oldNode, newNode);
        newParent.color = newParentColor;
        replaceChild(grandparent, oldNode, newParent);
    }

    void splitLeafNode(Node<Piece> oldNode, Node<Piece> newNode, int offset) {
        Node<Piece> grandparent = oldNode.parent;

        Piece oldPiece = oldNode.payload;
        int oldLength = oldPiece.getLength();

        Piece leftPiece = new Piece(oldPiece.getBuffer(), oldPiece.getStart(), offset);
        Piece rightPiece = new Piece(oldPiece.getBuffer(), oldPiece.getStart() + offset, oldLength - offset);

        Node<Piece>leftNode = createLeafNode(leftPiece);
        Node<Piece> rightNode = createLeafNode(rightPiece);

        leftNode.color = Color.RED;
        rightNode.color = Color.RED;
        newNode.color = Color.RED;

        Node<Piece> rightSubTree = createInternalNode(newNode, rightNode);
        rightSubTree.color = Color.RED;

        Node<Piece> newParent = createInternalNode(leftNode, rightSubTree);
        newParent.color = oldNode.color;
        replaceChild(grandparent, oldNode, newParent);
    }

    record NodeOffset(Node<Piece> node, int offset) {}
    Optional<NodeOffset> findNodeAndOffset(int position) {
        if (root == null) return Optional.empty();
        Node<Piece> node = root;

        position = Math.min(treeLength(), Math.max(position, 0));

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

    record NodeRange(NodeOffset start, NodeOffset end) {}
    Optional<NodeRange> findNodeAndRange(int position, int removeLength) {
        if (root == null || removeLength <= 0) return Optional.empty();

        int treeLen = treeLength();
        if (position < 0 || position >= treeLen) return Optional.empty();

        int startPos = Math.max(0, position);
        int endPos = Math.min(treeLength(), position + removeLength);

        NodeOffset start = findNodeAndOffset(startPos).orElse(null);
        NodeOffset end = findNodeAndOffset(endPos).orElse(null);

        return (start != null && end != null) ? Optional.of(new NodeRange(start, end)) : Optional.empty();
    }

    Node<Piece> removeBetweenLeaves(Node<Piece> startLeaf, Node<Piece> endLeaf) {
        if (startLeaf == null || endLeaf == null) throw new IllegalArgumentException("Illegal remove between leaves");

        Node<Piece> curLeaf = nextLeaf(startLeaf);
        if (curLeaf == null || curLeaf == endLeaf) return startLeaf;

        while (curLeaf != null && curLeaf != endLeaf) {
            Node<Piece> nextLeaf = nextLeaf(curLeaf);

            Node<Piece> removedLeaf = curLeaf;
            Node<Piece> parent = removedLeaf.parent;
            replaceChild(parent, removedLeaf, null);

            if (removedLeaf.isBlack()) {
                Node<Piece> problemNode = findNodeForFixup(removedLeaf);
                if (problemNode != null) removeFixup(problemNode);
            }
            curLeaf = nextLeaf;
        }
        return startLeaf;
    }

    private Node<Piece> leftmost(Node<Piece> node) {
        Node<Piece> cur = node;
        while (cur != null && !cur.isLeaf()) {
            cur = cur.left;
        }
        return cur;
    }

    private Node<Piece> nextLeaf(Node<Piece> leaf) {
        if (leaf == null) return null;

        Node<Piece> p = leaf.parent;
        if (p == null) return null;

        // get the right sibling of the current left node
        if (p.left == leaf) return leftmost(p.right);

        // traverse up the tree until the next leaf node is found
        Node<Piece> cur = leaf;
        Node<Piece> anc = p;
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
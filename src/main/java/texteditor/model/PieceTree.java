package texteditor.model;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

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


        if (node.isLeaf()) {
            node.length = (node.payload != null) ? node.payload.getLength() : 0;
        } else {
            int leftLen = (node.left != null) ? node.left.length : 0;
            int rightLen = (node.right != null) ? node.right.length : 0;
            node.length = leftLen + rightLen;
        }
    }

    @Override
    protected int payloadLength(Piece piece) {
        return (piece != null) ? piece.getLength() : 0;
    }

    private void collectText(Node<Piece> node, StringBuilder stringBuilder, String originalBuffer, StringBuilder addBuffer) {
        if (node == null) {
            return;
        }
        if (node.isLeaf()) {
            String text = node.payload.getText(originalBuffer, addBuffer);
            stringBuilder.append(text);
        } else {
            collectText(node.left, stringBuilder, originalBuffer, addBuffer);
            collectText(node.right, stringBuilder, originalBuffer, addBuffer);
        }
    }

    public String getText(String originalBuffer, StringBuilder addBuffer) {
        StringBuilder sb = new StringBuilder(treeLength());
        collectText(root, sb, originalBuffer, addBuffer);
        return sb.toString();
    }

    private void collectPieces(Node<Piece> node, List<Piece> out) {
        if (node == null) return;
        if (node.isLeaf()) out.add(node.payload);
        else {
            collectPieces(node.left, out);
            collectPieces(node.right, out);
        }
    }

    public List<Piece> toPieceList() {
        List<Piece> out = new ArrayList<>();
        collectPieces(root, out);
        return out;
    }

    private void addSiblingNode(Node<Piece> oldNode, Node<Piece> newNode, boolean newOnLeft) {
        Node<Piece> grandparent = oldNode.parent; // this was originally the parent of the node that needs a sibling

        newNode.color = Color.RED;
        Color newParentColor = oldNode.color;
        oldNode.color = Color.RED;


        Node<Piece> newParent = newOnLeft ? createInternalNode(newNode, oldNode) : createInternalNode(oldNode, newNode);
        newParent.color = newParentColor;
        replaceChild(grandparent, oldNode, newParent);
    }

    private void splitLeafNode(Node<Piece> oldNode, Node<Piece> newNode, int offset) {
        Node<Piece> grandparent = oldNode.parent;

        Piece oldPiece = oldNode.payload;
        int oldLength = oldPiece.getLength();

        Piece leftPiece = new Piece(oldPiece.getSource(), oldPiece.getStart(), offset);
        Piece rightPiece = new Piece(oldPiece.getSource(), oldPiece.getStart() + offset, oldLength - offset);

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

    private record NodeOffset(Node<Piece> node, int offset) {}

    private NodeOffset findNodeAndOffset(int position) {
        position = Math.min(treeLength(), Math.max(position, 0));

        Node<Piece> node = root;
        if (node == null) return null;

        while (!Objects.requireNonNull(node).isLeaf()) {
            int leftLen = (node.left != null) ? node.left.length : 0;
            if (position < leftLen) {
                node = node.left;
            } else {
                position -= leftLen;
                node = node.right;
            }
        }
        return new NodeOffset(node, position);
    }

    @Override
    protected Node<Piece> insertRecursive(Node<Piece> node, int position, Piece pieceToInsert) {
        if (node.isLeaf()) {
            Piece old = node.payload;
            int oldLen = old.getLength();
            int offset = Math.max(0, Math.min(position, oldLen));

            if (offset == 0) {
                Node<Piece> newLeaf = createLeafNode(pieceToInsert);
                addSiblingNode(node, newLeaf, true);
                return newLeaf;

            } else if (offset == oldLen) {
                // new piece after current leaf
                Node<Piece> newNode = createLeafNode(pieceToInsert);
                addSiblingNode(node, newNode, false);
                return newNode;

            } else {
                Node<Piece> newNode = createLeafNode(pieceToInsert);
                splitLeafNode(node, newNode, offset);
                return newNode;
            }
        }

        int leftLen = (node.left != null) ? node.left.length : 0;

        if (position < leftLen) {
            return insertRecursive(node.left, position, pieceToInsert);
        } else {
            return insertRecursive(node.right, position - leftLen, pieceToInsert);
        }
    }

    @Override
    protected Node<Piece> removeRecursive(Node<Piece> node, int position, int removeLength) {
        if (node == null || removeLength <= 0) return node;

        if (node.isLeaf()) {
            int pieceLen = node.payload.getLength();
            int start = Math.max(0, Math.min(position, pieceLen)); // start point of the deletion
            int end = Math.max(0, Math.min(position + removeLength, pieceLen)); // end point of the deletion
            if (start >= end) return node; // nothing to remove in this leaf

            int leftLen = start; // length of the left portion of the string after deletion
            int rightLen = pieceLen - end; // length of the right portion of the string after deletion

            Node<Piece> grandparent = node.parent;

            if (leftLen > 0 && rightLen > 0) {
                Piece leftPiece = new Piece(node.payload.getSource(), node.payload.getStart(), leftLen);
                Piece rightPiece = new Piece(node.payload.getSource(), node.payload.getStart() + end, rightLen);

                Node<Piece> leftNode = createLeafNode(leftPiece);
                Node<Piece> rightNode = createLeafNode(rightPiece);
                Node<Piece> newParent = createInternalNode(leftNode, rightNode);

                newParent.color = node.color;

                replaceChild(grandparent,node, newParent);
                return null;

            } else if (leftLen > 0) {
                Piece leftPiece = new Piece(node.payload.getSource(), node.payload.getStart(), leftLen);
                Node<Piece> leftNode = createLeafNode(leftPiece);
                recompute(leftNode);

                leftNode.color = node.color;

                replaceChild(grandparent, node, leftNode);
                return null;

            } else if (rightLen > 0) {
                Piece rightPiece = new Piece(node.payload.getSource(), node.payload.getStart() + end, rightLen);
                Node<Piece> rightNode = createLeafNode(rightPiece);
                recompute(rightNode);

                rightNode.color = node.color;

                replaceChild(grandparent, node, rightNode);
                return null;
            } else {
                replaceChild(grandparent, node, null);
                return node;
            }
        }

        int leftSubLen = (node.left != null) ? node.left.length : 0;
        Node<Piece> removedNode = null;

        if (position + removeLength <= leftSubLen) {
            // deletion entirely in left subtree
            removedNode = removeRecursive(node.left, position, removeLength);
        } else if (position >= leftSubLen) {
            // entirely in right subtree
            removedNode = removeRecursive(node.right, position - leftSubLen, removeLength);
        } else {

            int removeFromLeft = leftSubLen - position;
            Node<Piece> leftRemoved = removeRecursive(node.left, position, removeFromLeft);
            int remaining = removeLength - removeFromLeft;
            Node<Piece> rightRemoved = removeRecursive(node.right, 0, remaining);

            removedNode = (leftRemoved != null) ? leftRemoved : rightRemoved; // in this case we'll delete the left if there was a spanning deletion
        }

        recompute(node); // recalculate the node's length since children may have changed
        Node<Piece> grandparent = node.parent;

        return removedNode;
    }

    public boolean isValidRedBlack() {
        if (root != null && root.isRed()) return false;  // Root must be black
        return checkRedBlackProperties(root) != -1;
    }

    private int checkRedBlackProperties(Node node) {
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
package texteditor.model;

import java.util.ArrayList;
import java.util.List;
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
        if (node == null) {return;}
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

    private Optional<NodeOffset> findNodeAndOffset(int position) {
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

    @Override
    protected Node<Piece> insertRecursive(int position, Piece pieceToInsert) {
        Optional<NodeOffset> result = findNodeAndOffset(position);

        if (result.isEmpty()) {
            this.root = createLeafNode(pieceToInsert);
            this.root.color = Color.BLACK;
            return this.root;
        }

        NodeOffset nodeAndOffset = result.get();
        Node <Piece> node = nodeAndOffset.node;
        int offset = nodeAndOffset.offset;

        Piece old = node.payload;
        int oldLen = old.getLength();
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

    private record NodeRange(NodeOffset start, NodeOffset end) {}

    private Optional<NodeRange> findNodeAndRange(int position, int removeLength) {
        if (root == null || removeLength <= 0) return Optional.empty();

        int treeLen = treeLength();
        if (position < 0 || position >= treeLen) return Optional.empty();

        int startPos = Math.max(0, position);
        int endPos = Math.min(treeLength(), position + removeLength);

        NodeOffset start = findNodeAndOffset(startPos).orElse(null);
        NodeOffset end = findNodeAndOffset(endPos).orElse(null);

        return (start != null && end != null) ? Optional.of(new NodeRange(start, end)) : Optional.empty();
    }

    @Override
    protected Optional<Node<Piece>> removeRecursive(int position, int removeLength) {
        if (removeLength <= 0) throw new IllegalArgumentException("Illegal remove length: " + removeLength);
        if (root == null) throw new IllegalStateException("Tree is empty");

        Optional<NodeRange> result = findNodeAndRange(position, removeLength);
        if (result.isEmpty()) {
            throw new IndexOutOfBoundsException("Invalid deletion range: pos=" + position + ", len=" + removeLength);
        }

        NodeOffset start = result.get().start();
        NodeOffset end = result.get().end();

        if (start.node() == end.node()) {
            Node<Piece> leaf = start.node;
            Piece piece = leaf.payload;

            int leftLen = start.offset();
            int rightLen = piece.getLength() - end.offset();

            if (leftLen > 0 && rightLen > 0) {
                Piece leftPiece = new Piece(piece.getSource(), piece.getStart(), leftLen);
                Piece rightPiece = new Piece(piece.getSource(), piece.getStart() + end.offset(), rightLen);

                Node<Piece> leftNode = createLeafNode(leftPiece);
                Node<Piece> rightNode = createLeafNode(rightPiece);
                Node<Piece> newParent = createInternalNode(leftNode, rightNode);

                newParent.color = leaf.color;

                replaceChild(leaf.parent, leaf, newParent);
                return Optional.empty();
            } else if (leftLen > 0) {
                Piece leftPiece = new Piece(piece.getSource(), piece.getStart(), leftLen);
                Node<Piece> leftNode = createLeafNode(leftPiece);
                recompute(leftNode);

                leftNode.color = leaf.color;

                replaceChild(leaf.parent, leaf, leftNode);
                return Optional.empty();

            } else if (rightLen > 0) {
                Piece rightPiece = new Piece(piece.getSource(), piece.getStart() + end.offset(), rightLen);
                Node<Piece> rightNode = createLeafNode(rightPiece);
                recompute(rightNode);

                rightNode.color = leaf.color;

                replaceChild(leaf.parent, leaf, rightNode);
                return Optional.empty();
            } else {
                replaceChild(leaf.parent, leaf, null);
                return Optional.of(leaf);
            }
        }
        // TODO:  create a version that grabs the interior nodes prior to trimming start and end nodes, and removes them
        Node<Piece> startLeaf = start.node();
        Node<Piece> endLeaf = end.node();

        if (start.offset < startLeaf.payload.getLength()) {
            Piece leftPiece = new Piece(startLeaf.payload.getSource(), startLeaf.payload.getStart(), start.offset());
            if (leftPiece.getLength() > 0) {
                Node<Piece> leftNode = createLeafNode(leftPiece);
                replaceChild(startLeaf.parent, startLeaf, leftNode);
                startLeaf = leftNode;
            } else {
                replaceChild(startLeaf.parent, startLeaf, null);
            }

        }

        int rightLen = endLeaf.payload.getLength() - end.offset();
        if (rightLen > 0) {
            Piece rightPiece = new Piece(endLeaf.payload.getSource(), endLeaf.payload.getStart() + end.offset(), rightLen);
            Node<Piece> rightNode = createLeafNode(rightPiece);
            replaceChild(endLeaf.parent, endLeaf, rightNode);
            endLeaf = rightNode;
        } else {
            replaceChild(endLeaf.parent, endLeaf, null);
        }

        Node<Piece> returnLeaf = removeBetweenLeaves(startLeaf, endLeaf);
        bubbleRecompute(startLeaf);
        bubbleRecompute(endLeaf);
        return Optional.of(returnLeaf);

    }

    private Node<Piece> removeBetweenLeaves(Node<Piece> startLeaf, Node<Piece> endLeaf) {
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

    private Node<Piece> trimLeaf(Node<Piece> leaf, int keepStart, int keepEnd) {
        return null;
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
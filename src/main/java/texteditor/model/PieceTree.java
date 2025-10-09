package texteditor.model;

import java.util.Objects;
import java.util.Optional;
import java.util.OptionalInt;

public class PieceTree extends RBTree<PieceTree.PieceNode, Piece> {

    public static class PieceNode extends Node<PieceNode, Piece> {
        private int newlineCount;
        PieceNode(Piece payload) { super(payload); }
        PieceNode(PieceNode left, PieceNode right) {
            super(left, right);
            if (left != null) left.parent = this;
            if (right != null) right.parent = this;
        }

        @Override
        public boolean isLeaf() { return payload != null; }

        public int getNewlineCount() { return newlineCount; }
    }

    public PieceTree(Piece initialPiece) {
        if (initialPiece != null) {
            this.root = createLeafNode(initialPiece);
            this.root.color = Color.BLACK;
        }
    }
    public PieceTree() {this(null);}
    @Override
    protected int treeLength() { return (root != null) ? root.length : 0; }
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
        bubbleRecompute(node);
        return node;
    }
    @Override
    protected PieceNode createInternalNode(PieceNode left, PieceNode right) {
        PieceNode node = new PieceNode(left, right);
        bubbleRecompute(node);
        return node;
    }
    @Override
    protected void recompute(PieceNode node) {
        if (node == null) return;
        if (node.isLeaf()) {
            node.length = (node.payload != null) ? node.payload.getLength() : 0;
            node.newlineCount = ( node.payload != null) ? node.payload.getLineCount() : 0;
        } else {
            node.length =
                    (node.left  != null ? node.left.length : 0) +
                            (node.right != null ? node.right.length : 0);
            node.newlineCount =
                    (node.left  != null ? node.left.newlineCount : 0) +
                            (node.right != null ? node.right.newlineCount : 0);
        }
    }

    @Override
    protected void addSiblingNode(PieceNode originalSiblingNode, PieceNode newSiblingNode, boolean newOnLeft) {
        PieceNode grandparent = originalSiblingNode.parent; // this was originally the parent of the node that needs a sibling

        newSiblingNode.color = Color.RED;
        Color newParentColor = originalSiblingNode.color;
        originalSiblingNode.color = Color.RED;

        PieceNode newParent = newOnLeft ?
                createInternalNode(newSiblingNode, originalSiblingNode) :
                createInternalNode(originalSiblingNode, newSiblingNode);

        newParent.color = newParentColor;
        replaceChild(grandparent, originalSiblingNode, newParent);
    }

    public void splitLeafNode(PieceNode nodeToSplit, PieceNode newNode, int localOffset) {
        PieceNode grandparent = nodeToSplit.parent;

        Piece oldPiece = nodeToSplit.payload;

        int oldLength = oldPiece.getLength();

        Piece leftPiece = new Piece(oldPiece.getBuffer(), oldPiece.getStart(), localOffset);
        Piece rightPiece = new Piece(oldPiece.getBuffer(), oldPiece.getStart() + localOffset, oldLength - localOffset);

        PieceNode leftNode = createLeafNode(leftPiece);
        PieceNode rightNode = createLeafNode(rightPiece);

        leftNode.color = Color.RED;
        rightNode.color = Color.RED;
        newNode.color = Color.RED;

        PieceNode rightSubTree = createInternalNode(newNode, rightNode);
        rightSubTree.color = Color.RED;

        PieceNode newParent = createInternalNode(leftNode, rightSubTree);
        newParent.color = nodeToSplit.color;
        replaceChild(grandparent, nodeToSplit, newParent);
    }

    record NodeLocation(PieceNode node, int localOffset) {}

    Optional<NodeLocation> getNodeLocation(int globalOffset) {
        if (this.root == null) return Optional.empty();

        if (globalOffset < 0 || globalOffset > treeLength()) {
            throw new IndexOutOfBoundsException(
                    "globalOffset " + globalOffset + " out of bounds [0, " + treeLength() + ")"
            );
        }

        PieceNode node = this.root;
        int offset = globalOffset;

        while (!Objects.requireNonNull(node).isLeaf()) {
            int leftLen = (node.left != null) ? node.left.length : 0;
            if (offset < leftLen) {
                node = node.left;
            } else {
                offset -= leftLen;
                node = node.right;
            }
        }
        return Optional.of(new NodeLocation(node, offset));
    }

    int getGlobalOffsetAtNodeLocation(NodeLocation nodeLocation) {
        if (nodeLocation == null || nodeLocation.node == null) throw new IllegalArgumentException("Invalid node location");

        int globalOffset = nodeLocation.localOffset();
        PieceNode currentNode = nodeLocation.node();

        while (currentNode != null) {
            PieceNode parentNode = currentNode.parent;
            if (parentNode != null && currentNode == parentNode.right) {
                globalOffset += (parentNode.left != null) ? parentNode.left.length : 0;
            }
            currentNode = parentNode;
        }

        return globalOffset;
    }

    public OptionalInt getGlobalOffsetOfLine(int lineNumber) {
        int lineIndex = lineNumber - 1;
        if (root == null || lineIndex < 0 || lineIndex >= root.newlineCount) return OptionalInt.empty();

        PieceNode currentNode = root;
        int baseOffset = 0;

        while (!Objects.requireNonNull(currentNode).isLeaf()) {
            int leftSubtreeNewlineCount = (currentNode.left != null) ? currentNode.left.newlineCount : 0;
            if (lineIndex < leftSubtreeNewlineCount) {
                currentNode = currentNode.left;
            } else {
                int leftSubtreeLength = (currentNode.left != null) ? currentNode.left.length : 0;
                lineIndex -= leftSubtreeNewlineCount;
                baseOffset += leftSubtreeLength;
                currentNode = currentNode.right;
            }
        }

        PieceNode currentLeaf = currentNode;
        while (currentLeaf != null) {
            Piece payload = currentLeaf.payload;
            for (int i = 0; i < currentLeaf.length; i++) {
                if (payload.getChar(i) == '\n') {
                    if (lineIndex == 0) return OptionalInt.of(baseOffset + i);
                    lineIndex--;
                }
            }
            baseOffset += currentLeaf.length;
            currentLeaf = nextLeaf(currentLeaf);
        }
        return OptionalInt.empty();
    }

    record NodeRange(NodeLocation startLocation, NodeLocation endLocation) {}

    NodeRange getNodeRange(int globalOffset, int removeLength) {
        if (root == null) throw new IllegalStateException("Cannot get range from empty tree");
        if (removeLength <= 0) throw new IllegalArgumentException("remove length must be positive");

        if (globalOffset < 0 || globalOffset >= treeLength()) throw new IllegalArgumentException("position must be between 0 and " + (treeLength() - 1));

        int endPos = Math.min(treeLength(), globalOffset + removeLength);

        NodeLocation startLocation = getNodeLocation(globalOffset).get();
        NodeLocation endLocation;
        if (endPos == treeLength()) {
            PieceNode lastLeaf = lastLeaf();
            endLocation = new NodeLocation(lastLeaf, lastLeaf.length);
        } else {
            endLocation = getNodeLocation(endPos).get();
        }

        return (startLocation != null && endLocation != null) ? new NodeRange(startLocation, endLocation) : null;
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


    public String getTreeText() {
        StringBuilder sb = new StringBuilder(treeLength());
        getTextHelper(this.root, sb);
        return sb.toString();
    }

    private void getTextHelper(PieceTree.PieceNode node, StringBuilder stringBuilder) {
        if (node == null) {return;}
        if (node.isLeaf()) {
            String text = node.payload.getText();
            stringBuilder.append(text);
        } else {
            getTextHelper(node.left, stringBuilder);
            getTextHelper(node.right, stringBuilder);
        }
    }


    public boolean isValidRedBlack() {
        if (root != null && root.isRed()) return false;  // Root must be black
        return checkRedBlackProperties(root) != -1;
    }

    private int checkRedBlackProperties(PieceNode node)  {
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

        if ( rightHeight == -1 || leftHeight != rightHeight) {
            return -1;  // Black height violation
        }

        return leftHeight + (node.isBlack() ? 1 : 0);
    }

}
package texteditor.model;

import java.util.*;

public class PieceTable extends RBTree<PieceTable.PieceNode, Piece> {

    private final OriginalBuffer originalBuffer;
    private final AddBuffer addBuffer;
    private int length;

    public static class PieceNode extends Node<PieceTable.PieceNode, Piece> {
        private int newlineCount;
        PieceNode(Piece payload) { super(payload); }
        PieceNode(PieceTable.PieceNode left, PieceTable.PieceNode right) {
            super(left, right);
            if (left != null) left.parent = this;
            if (right != null) right.parent = this;
        }

        @Override
        public boolean isLeaf() { return payload != null; }

        public int getNewlineCount() { return newlineCount; }
    }

    public PieceTable(String originalText) {
        this.originalBuffer = new OriginalBuffer(originalText);
        this.addBuffer = new AddBuffer();

        if (!originalText.isEmpty()) {
            Piece piece = new Piece(originalBuffer, 0, originalText.length());
            insertHelper(0, piece);
        }
        this.length = getTreeLength();
    }

    @Override
    protected int treeLength() { return (root != null) ? root.length : 0; }

    @Override
    protected void setRoot(PieceTable.PieceNode node) {
        this.root = node;
        this.root.color = Color.BLACK;
        recompute(this.root);
    }

    @Override
    protected PieceTable.PieceNode getRoot() { return this.root; }

    @Override
    protected PieceTable.PieceNode createLeafNode(Piece payload) {
        PieceTable.PieceNode node = new PieceTable.PieceNode(payload);
        bubbleRecompute(node);
        return node;
    }

    @Override
    protected PieceTable.PieceNode createInternalNode(PieceTable.PieceNode left, PieceTable.PieceNode right) {
        PieceTable.PieceNode node = new PieceTable.PieceNode(left, right);
        bubbleRecompute(node);
        return node;
    }

    @Override
    protected void recompute(PieceTable.PieceNode node) {
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
    protected void addSiblingNode(PieceTable.PieceNode originalSiblingNode, PieceTable.PieceNode newSiblingNode, boolean newOnLeft) {
        PieceTable.PieceNode grandparent = originalSiblingNode.parent; // this was originally the parent of the node that needs a sibling

        newSiblingNode.color = Color.RED;
        Color newParentColor = originalSiblingNode.color;
        originalSiblingNode.color = Color.RED;

        PieceTable.PieceNode newParent = newOnLeft ?
                createInternalNode(newSiblingNode, originalSiblingNode) :
                createInternalNode(originalSiblingNode, newSiblingNode);

        newParent.color = newParentColor;
        replaceChild(grandparent, originalSiblingNode, newParent);
    }

    public void splitLeafNode(PieceTable.PieceNode nodeToSplit, PieceTable.PieceNode newNode, int localOffset) {
        PieceTable.PieceNode grandparent = nodeToSplit.parent;

        Piece oldPiece = nodeToSplit.payload;

        int oldLength = oldPiece.getLength();

        Piece leftPiece = new Piece(oldPiece.getBuffer(), oldPiece.getStart(), localOffset);
        Piece rightPiece = new Piece(oldPiece.getBuffer(), oldPiece.getStart() + localOffset, oldLength - localOffset);

        PieceTable.PieceNode leftNode = createLeafNode(leftPiece);
        PieceTable.PieceNode rightNode = createLeafNode(rightPiece);

        leftNode.color = Color.RED;
        rightNode.color = Color.RED;
        newNode.color = Color.RED;

        PieceTable.PieceNode rightSubTree = createInternalNode(newNode, rightNode);
        rightSubTree.color = Color.RED;

        PieceTable.PieceNode newParent = createInternalNode(leftNode, rightSubTree);
        newParent.color = nodeToSplit.color;
        replaceChild(grandparent, nodeToSplit, newParent);
    }

    record NodeLocation(PieceTable.PieceNode node, int localOffset) {}

    PieceTable.NodeLocation getNodeLocation(int globalOffset) {
        if (this.root == null) throw new IllegalStateException("tree is empty");

        if (globalOffset < 0 || globalOffset > treeLength()) {
            throw new IndexOutOfBoundsException(
                    "globalOffset " + globalOffset + " out of bounds [0, " + treeLength() + ")"
            );
        }

        PieceTable.PieceNode node = this.root;
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
        return new PieceTable.NodeLocation(node, offset);
    }

    int getGlobalOffsetAtNodeLocation(PieceTable.NodeLocation nodeLocation) {
        if (nodeLocation == null || nodeLocation.node == null) throw new IllegalArgumentException("Invalid node location");

        int globalOffset = nodeLocation.localOffset();
        PieceTable.PieceNode currentNode = nodeLocation.node();

        while (currentNode != null) {
            PieceTable.PieceNode parentNode = currentNode.parent;
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

        PieceTable.PieceNode currentNode = root;
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

        PieceTable.PieceNode currentLeaf = currentNode;
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

    record NodeRange(PieceTable.NodeLocation startLocation, PieceTable.NodeLocation endLocation) {}

    PieceTable.NodeRange getNodeRange(int globalOffset, int removeLength) {
        if (root == null) throw new IllegalStateException("Cannot get range from empty tree");
        if (removeLength <= 0) throw new IllegalArgumentException("remove length must be positive");

        if (globalOffset < 0 || globalOffset >= treeLength()) throw new IllegalArgumentException("position must be between 0 and " + (treeLength() - 1));

        int endPos = Math.min(treeLength(), globalOffset + removeLength);

        PieceTable.NodeLocation startLocation = getNodeLocation(globalOffset);
        PieceTable.NodeLocation endLocation;
        if (endPos == treeLength()) {
            PieceTable.PieceNode lastLeaf = lastLeaf();
            endLocation = new PieceTable.NodeLocation(lastLeaf, lastLeaf.length);
        } else {
            endLocation = getNodeLocation(endPos);
        }

        return (startLocation != null && endLocation != null) ? new PieceTable.NodeRange(startLocation, endLocation) : null;
    }

    PieceTable.PieceNode removeBetweenLeaves(PieceTable.PieceNode startLeaf, PieceTable.PieceNode endLeaf) {
        if (startLeaf == null || endLeaf == null) throw new IllegalArgumentException("Illegal remove between leaves");

        PieceTable.PieceNode curLeaf = nextLeaf(startLeaf);
        if (curLeaf == null || curLeaf == endLeaf) return startLeaf;

        while (curLeaf != null && curLeaf != endLeaf) {
            PieceTable.PieceNode nextLeaf = nextLeaf(curLeaf);

            PieceTable.PieceNode removedLeaf = curLeaf;
            PieceTable.PieceNode parent = removedLeaf.parent;
            replaceChild(parent, removedLeaf, null);

            if (removedLeaf.isBlack()) {
                PieceTable.PieceNode problemNode = findNodeForFixup(removedLeaf);
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

    private void getTextHelper(PieceTable.PieceNode node, StringBuilder stringBuilder) {
        if (node == null) {return;}
        if (node.isLeaf()) {
            String text = node.payload.getText();
            stringBuilder.append(text);
        } else {
            getTextHelper(node.left, stringBuilder);
            getTextHelper(node.right, stringBuilder);
        }
    }

    public int getTreeLength() {
        return this.treeLength();
    }

    public void recalculateLength() {
        this.length = getTreeLength();
    }

    public String getText() {
        return this.getTreeText();
    }

    private int toIndex(int pos) {
        return pos - 1;
    }

    private int toPosition(int index) {
        return index + 1;
    }

    public void insert(int position, String text) {
        if (text == null || text.isEmpty()) return;

        int textLength = text.length();
        addBuffer.append(text);
        Piece newPiece = new Piece(addBuffer, addBuffer.length() - textLength, textLength);
        insertHelper(position, newPiece);
        recalculateLength();
    }

    private void insertHelper(int position, Piece pieceToInsert) {
        if (this.getRoot() == null) {
            if (position != 0) {
                throw new IllegalArgumentException(("Can only insert at position 0 in empty this"));
            }
            this.setRoot(this.createLeafNode(pieceToInsert));
            return;
        }

        if (position == getTreeLength()) {
            PieceTable.PieceNode lastLeaf = this.lastLeaf();
            PieceTable.PieceNode newNode = this.createLeafNode(pieceToInsert);
            this.addSiblingNode(lastLeaf, newNode, false);
            this.insertFixup(newNode);
            this.bubbleRecompute(newNode);
            return;
        }

        PieceTable.NodeLocation result = this.getNodeLocation(position);

        PieceTable.PieceNode node = result.node();
        int offset = result.localOffset();

        Piece oldPiece = node.payload;
        if (offset == 0) {
            PieceTable.PieceNode newLeaf = this.createLeafNode(pieceToInsert);
            this.addSiblingNode(node, newLeaf, true);
            this.insertFixup(newLeaf);
            this.bubbleRecompute(newLeaf);
        } else if (offset == oldPiece.getLength()) {
            // new piece after current leaf
            PieceTable.PieceNode newNode = this.createLeafNode(pieceToInsert);
            this.addSiblingNode(node, newNode, false);
            this.insertFixup(newNode);
            this.bubbleRecompute(newNode);
        } else {
            PieceTable.PieceNode newNode = this.createLeafNode(pieceToInsert);
            this.splitLeafNode(node, newNode, offset);
            this.insertFixup(newNode);
            this.bubbleRecompute(newNode);
        }
    }

    public void remove(int position, int length) {
        if (length <= 0 || position < 0 || position >= this.length) return;

        if (this.getRoot() == null) return;

        if (position + length > this.length) {
            length = this.length - position;
        }

        removeHelper(position, length);
        recalculateLength();
    }

    private void removeHelper(int position, int removeLength) {
        if (removeLength <= 0) throw new IllegalArgumentException("Remove length must be positive: " + removeLength);
        if (this.getRoot() == null) throw new IllegalStateException("Cannot remove from empty tree");
        if (position < 0 || position >= this.treeLength()) {
            throw new IndexOutOfBoundsException("Position " + position + " is out of bounds of this length " + this.treeLength());
        }

        PieceTable.NodeRange result = this.getNodeRange(position, removeLength);
        if (result == null) {
            throw new IndexOutOfBoundsException("Invalid deletion range: pos=" + position + ", len=" + removeLength);
        }

        PieceTable.NodeLocation start = result.startLocation();
        PieceTable.NodeLocation end = result.endLocation();

        if (start.node() == end.node()) {
            PieceTable.PieceNode leaf = start.node();
            Piece piece = leaf.payload;

            int leftLen = start.localOffset();
            int rightLen = piece.getLength() - end.localOffset();

            if (leftLen > 0 && rightLen > 0) {
                Piece leftPiece = new Piece(piece.getBuffer(), piece.getStart(), leftLen);
                Piece rightPiece = new Piece(piece.getBuffer(), piece.getStart() + end.localOffset(), rightLen);

                PieceTable.PieceNode leftNode = this.createLeafNode(leftPiece);
                PieceTable.PieceNode rightNode = this.createLeafNode(rightPiece);
                PieceTable.PieceNode newParent = this.createInternalNode(leftNode, rightNode);

                newParent.color = leaf.color;

                this.replaceChild(leaf.parent, leaf, newParent);
                return;
            } else if (leftLen > 0) {
                Piece leftPiece = new Piece(piece.getBuffer(), piece.getStart(), leftLen);
                PieceTable.PieceNode leftNode = this.createLeafNode(leftPiece);
                this.recompute(leftNode);

                leftNode.color = leaf.color;

                this.replaceChild(leaf.parent, leaf, leftNode);
                return;
            } else if (rightLen > 0) {
                Piece rightPiece = new Piece(piece.getBuffer(), piece.getStart() + end.localOffset(), rightLen);
                PieceTable.PieceNode rightNode = this.createLeafNode(rightPiece);
                this.recompute(rightNode);

                rightNode.color = leaf.color;

                this.replaceChild(leaf.parent, leaf, rightNode);
                return;
            } else {
                this.replaceChild(leaf.parent, leaf, null);
                if (leaf.isBlack()) {
                    PieceTable.PieceNode problemNode = this.findNodeForFixup(leaf);
                    if (problemNode != null) this.removeFixup(problemNode);
                }
                return;
            }
        }
        // TODO:  create a version that grabs the interior nodes prior to trimming startLocation and endLocation nodes, and removes them
        PieceTable.PieceNode startLeaf = start.node();
        PieceTable.PieceNode endLeaf = end.node();

        if (start.localOffset() < startLeaf.payload.getLength()) {
            Piece leftPiece = new Piece(startLeaf.payload.getBuffer(), startLeaf.payload.getStart(), start.localOffset());
            if (leftPiece.getLength() > 0) {
                PieceTable.PieceNode leftNode = this.createLeafNode(leftPiece);
                this.replaceChild(startLeaf.parent, startLeaf, leftNode);
                startLeaf = leftNode;
            } else {
                this.replaceChild(startLeaf.parent, startLeaf, null);
            }

        }

        int rightLen = endLeaf.payload.getLength() - end.localOffset();
        if (rightLen > 0) {
            Piece rightPiece = new Piece(endLeaf.payload.getBuffer(), endLeaf.payload.getStart() + end.localOffset(), rightLen);
            PieceTable.PieceNode rightNode = this.createLeafNode(rightPiece);
            this.replaceChild(endLeaf.parent, endLeaf, rightNode);
            endLeaf = rightNode;
        } else {
            this.replaceChild(endLeaf.parent, endLeaf, null);
        }

        PieceTable.PieceNode returnLeaf = this.removeBetweenLeaves(startLeaf, endLeaf);
        this.bubbleRecompute(startLeaf);
        this.bubbleRecompute(endLeaf);
        if (returnLeaf.isBlack()) {
            PieceTable.PieceNode problemNode = this.findNodeForFixup(returnLeaf);
            if (problemNode != null) this.removeFixup(problemNode);
            this.bubbleRecompute(startLeaf);
            this.bubbleRecompute(endLeaf);
        }
    }

    public int getLineCount() {
        int newlineCount = this.getRoot().getNewlineCount();

        OptionalInt positionOfLastNewLineChar = this.getGlobalOffsetOfLine(newlineCount);

        if (positionOfLastNewLineChar.isEmpty()) return 1;

        return (positionOfLastNewLineChar.getAsInt() == getTreeLength() - 1) ? newlineCount : newlineCount + 1;

    }

    public int getLineLength(int lineIndex) {
        return getLine(lineIndex).length();
    }

    public boolean isLastLine(int lineIndex) {
        return lineIndex == getLineCount() - 1;
    }

    public String getLine(int lineIndex) {
        Optional<String> res = getLineString(lineIndex);
        return res.orElse(null);
    }

    public LineComponents getLineComponents(int lineIndex) {
        Optional<String> res = getLineString(lineIndex);
        if (res.isPresent()) {
            return new LineComponents(res.get());
        } else {
            return null;
        }
    }

    Optional<String> getLineString(int lineIndex) {
        if (this.getRoot() == null) return Optional.empty();

        int totalLines = this.getRoot().getNewlineCount() + (this.treeLength() > 0 ? 1 : 0);
        if (lineIndex < 0 || lineIndex >= totalLines) return Optional.empty();

        // get startLocation position of the requested line
        int startPos;
        if (lineIndex == 0) {
            startPos = 0;
        } else {
            OptionalInt nthNewlinePos = this.getGlobalOffsetOfLine(lineIndex);
            if (nthNewlinePos.isEmpty()) return Optional.empty();
            startPos = nthNewlinePos.getAsInt() + 1;
        }

        // find leaf and localOffset for startPos
        PieceTable.NodeLocation nodeOffset = this.getNodeLocation(startPos);
        PieceTable.PieceNode node = nodeOffset.node();
        int offset = nodeOffset.localOffset();

        StringBuilder sb = new StringBuilder();
        PieceTable.PieceNode cur = node;
        int curOffset = offset;

        while (cur != null) {
            for (int i = curOffset; i < cur.length; i++) {
                char c = cur.payload.getChar(i);
                sb.append(c);
                if (c == '\n') {
                    return Optional.of(sb.toString());
                }
            }
            cur = this.nextLeaf(cur);
            curOffset = 0;
        }
        return Optional.of(sb.toString());
    }
}


package texteditor.model;

import java.util.*;

public class PieceTable {

    private final OriginalBuffer originalBuffer;
    private final AddBuffer addBuffer;
    private final PieceTree tree;
    private int length;

    public PieceTable(String originalText) {
        this.originalBuffer = new OriginalBuffer(originalText);
        this.addBuffer = new AddBuffer();
        this.tree = new PieceTree();

        if (!originalText.isEmpty()) {
            Piece piece = new Piece(originalBuffer, 0, originalText.length());
            insertHelper(0, piece);
        }
        this.length = getTreeLength();
    }

    public int getTreeLength() {
        return tree.treeLength();
    }

    public void recalculateLength() {
        this.length = getTreeLength();
    }

    public String getText() {
        return tree.getTreeText();
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
        if (tree.getRoot() == null) {
            if (position != 0) {
                throw new IllegalArgumentException(("Can only insert at position 0 in empty tree"));
            }
            tree.setRoot(tree.createLeafNode(pieceToInsert));
            return;
        }

        if (position == getTreeLength()) {
            PieceTree.PieceNode lastLeaf = tree.lastLeaf();
            PieceTree.PieceNode newNode = tree.createLeafNode(pieceToInsert);
            tree.addSiblingNode(lastLeaf, newNode, false);
            tree.insertFixup(newNode);
            tree.bubbleRecompute(newNode);
            return;
        }

        PieceTree.NodeLocation result = tree.getNodeLocation(position);

        PieceTree.PieceNode node = result.node();
        int offset = result.localOffset();

        Piece oldPiece = node.payload;
        if (offset == 0) {
            PieceTree.PieceNode newLeaf = tree.createLeafNode(pieceToInsert);
            tree.addSiblingNode(node, newLeaf, true);
            tree.insertFixup(newLeaf);
            tree.bubbleRecompute(newLeaf);
        } else if (offset == oldPiece.getLength()) {
            // new piece after current leaf
            PieceTree.PieceNode newNode = tree.createLeafNode(pieceToInsert);
            tree.addSiblingNode(node, newNode, false);
            tree.insertFixup(newNode);
            tree.bubbleRecompute(newNode);
        } else {
            PieceTree.PieceNode newNode = tree.createLeafNode(pieceToInsert);
            tree.splitLeafNode(node, newNode, offset);
            tree.insertFixup(newNode);
            tree.bubbleRecompute(newNode);
        }
    }

    public void remove(int position, int length) {
        if (length <= 0 || position < 0 || position >= this.length) return;

        if (tree.getRoot() == null) return;

        if (position + length > this.length) {
            length = this.length - position;
        }

        removeHelper(position, length);
        recalculateLength();
    }

    private void removeHelper(int position, int removeLength) {
        if (removeLength <= 0) throw new IllegalArgumentException("Remove length must be positive: " + removeLength);
        if (tree.getRoot() == null) throw new IllegalStateException("Cannot remove from empty tree");
        if (position < 0 || position >= tree.treeLength()) {
            throw new IndexOutOfBoundsException("Position " + position + " is out of bounds of tree length " + tree.treeLength());
        }

        PieceTree.NodeRange result = tree.getNodeRange(position, removeLength);
        if (result == null) {
            throw new IndexOutOfBoundsException("Invalid deletion range: pos=" + position + ", len=" + removeLength);
        }

        PieceTree.NodeLocation start = result.startLocation();
        PieceTree.NodeLocation end = result.endLocation();

        if (start.node() == end.node()) {
            PieceTree.PieceNode leaf = start.node();
            Piece piece = leaf.payload;

            int leftLen = start.localOffset();
            int rightLen = piece.getLength() - end.localOffset();

            if (leftLen > 0 && rightLen > 0) {
                Piece leftPiece = new Piece(piece.getBuffer(), piece.getStart(), leftLen);
                Piece rightPiece = new Piece(piece.getBuffer(), piece.getStart() + end.localOffset(), rightLen);

                PieceTree.PieceNode leftNode = tree.createLeafNode(leftPiece);
                PieceTree.PieceNode rightNode = tree.createLeafNode(rightPiece);
                PieceTree.PieceNode newParent = tree.createInternalNode(leftNode, rightNode);

                newParent.color = leaf.color;

                tree.replaceChild(leaf.parent, leaf, newParent);
                return;
            } else if (leftLen > 0) {
                Piece leftPiece = new Piece(piece.getBuffer(), piece.getStart(), leftLen);
                PieceTree.PieceNode leftNode = tree.createLeafNode(leftPiece);
                tree.recompute(leftNode);

                leftNode.color = leaf.color;

                tree.replaceChild(leaf.parent, leaf, leftNode);
                return;
            } else if (rightLen > 0) {
                Piece rightPiece = new Piece(piece.getBuffer(), piece.getStart() + end.localOffset(), rightLen);
                PieceTree.PieceNode rightNode = tree.createLeafNode(rightPiece);
                tree.recompute(rightNode);

                rightNode.color = leaf.color;

                tree.replaceChild(leaf.parent, leaf, rightNode);
                return;
            } else {
                tree.replaceChild(leaf.parent, leaf, null);
                if (leaf.isBlack()) {
                    PieceTree.PieceNode problemNode = tree.findNodeForFixup(leaf);
                    if (problemNode != null) tree.removeFixup(problemNode);
                }
                return;
            }
        }
        // TODO:  create a version that grabs the interior nodes prior to trimming startLocation and endLocation nodes, and removes them
        PieceTree.PieceNode startLeaf = start.node();
        PieceTree.PieceNode endLeaf = end.node();

        if (start.localOffset() < startLeaf.payload.getLength()) {
            Piece leftPiece = new Piece(startLeaf.payload.getBuffer(), startLeaf.payload.getStart(), start.localOffset());
            if (leftPiece.getLength() > 0) {
                PieceTree.PieceNode leftNode = tree.createLeafNode(leftPiece);
                tree.replaceChild(startLeaf.parent, startLeaf, leftNode);
                startLeaf = leftNode;
            } else {
                tree.replaceChild(startLeaf.parent, startLeaf, null);
            }

        }

        int rightLen = endLeaf.payload.getLength() - end.localOffset();
        if (rightLen > 0) {
            Piece rightPiece = new Piece(endLeaf.payload.getBuffer(), endLeaf.payload.getStart() + end.localOffset(), rightLen);
            PieceTree.PieceNode rightNode = tree.createLeafNode(rightPiece);
            tree.replaceChild(endLeaf.parent, endLeaf, rightNode);
            endLeaf = rightNode;
        } else {
            tree.replaceChild(endLeaf.parent, endLeaf, null);
        }

        PieceTree.PieceNode returnLeaf = tree.removeBetweenLeaves(startLeaf, endLeaf);
        tree.bubbleRecompute(startLeaf);
        tree.bubbleRecompute(endLeaf);
        if (returnLeaf.isBlack()) {
            PieceTree.PieceNode problemNode = tree.findNodeForFixup(returnLeaf);
            if (problemNode != null) tree.removeFixup(problemNode);
            tree.bubbleRecompute(startLeaf);
            tree.bubbleRecompute(endLeaf);
        }
    }


    public int getLineCount() {
        int newlineCount = tree.getRoot().getNewlineCount();

        OptionalInt positionOfLastNewLineChar = tree.getGlobalOffsetOfLine(newlineCount);

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
        if (tree.getRoot() == null) return Optional.empty();

        int totalLines = tree.getRoot().getNewlineCount() + (tree.treeLength() > 0 ? 1 : 0);
        if (lineIndex < 0 || lineIndex >= totalLines) return Optional.empty();

        // get startLocation position of the requested line
        int startPos;
        if (lineIndex == 0) {
            startPos = 0;
        } else {
            OptionalInt nthNewlinePos = tree.getGlobalOffsetOfLine(lineIndex);
            if (nthNewlinePos.isEmpty()) return Optional.empty();
            startPos = nthNewlinePos.getAsInt() + 1;
        }

        // find leaf and localOffset for startPos
        PieceTree.NodeLocation nodeOffset = tree.getNodeLocation(startPos);
        PieceTree.PieceNode node = nodeOffset.node();
        int offset = nodeOffset.localOffset();

        StringBuilder sb = new StringBuilder();
        PieceTree.PieceNode cur = node;
        int curOffset = offset;

        while (cur != null) {
            for (int i = curOffset; i < cur.length; i++) {
                char c = cur.payload.getChar(i);
                sb.append(c);
                if (c == '\n') {
                    return Optional.of(sb.toString());
                }
            }
            cur = tree.nextLeaf(cur);
            curOffset = 0;
        }
        return Optional.of(sb.toString());
    }

}


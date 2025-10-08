package texteditor.model;

import java.util.*;

public class PieceTable {

    private final OriginalBuffer originalBuffer;
    private final AddBuffer addBuffer;
    private final PieceTree tree;
    private int totalLength;

    public PieceTable(String originalText) {
        this.originalBuffer = new OriginalBuffer(originalText);
        this.addBuffer = new AddBuffer();
        // this.pieces = new ArrayList<>();
        this.tree = new PieceTree();

        if (!originalText.isEmpty()) {
            Piece piece = new Piece(originalBuffer, 0, originalText.length());
            insertHelper(0, piece);
            this.totalLength = piece.getLength();
        }
    }

    public void insert(int position, String text) {
        if (text == null || text.isEmpty()) return;

        int textLength = text.length();
        addBuffer.append(text);
        Piece newPiece = new Piece(addBuffer, addBuffer.length() - textLength, textLength);

        insertHelper(position, newPiece);
        totalLength += textLength;
    }

    private void insertHelper(int position, Piece pieceToInsert) {
        Optional<PieceTree.NodeLocation> result = tree.translateToNodeLocation(position);

        if (result.isEmpty()) {
            tree.setRoot(tree.createLeafNode(pieceToInsert));
            return;
        }

        PieceTree.PieceNode node = result.get().node();
        int offset = result.get().localOffset();

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
        if (length <= 0 || position < 0 || position >= totalLength) return;

        if (position + length > totalLength) {
            length = totalLength - position;  // trim to valid range
        }

        removeHelper(position, length);
        totalLength -= length;
    }

    private void removeHelper(int position, int removeLength) {
        if (removeLength <= 0) throw new IllegalArgumentException("Illegal remove length: " + removeLength);
        if (tree.root == null) throw new IllegalStateException("Tree is empty");

        PieceTree.NodeRange result = tree.findNodeAndRange(position, removeLength);
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

    public String getText() {
        StringBuilder sb = new StringBuilder(tree.treeLength());
        getTextHelper(tree.root, sb);
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

    private void collectPieces(PieceTree.PieceNode node, List<Piece> out) {
        if (node == null) return;
        if (node.isLeaf()) out.add(node.payload);
        else {
            collectPieces(node.left, out);
            collectPieces(node.right, out);
        }
    }

    public List<Piece> toPieceList() {
        List<Piece> out = new ArrayList<>();
        collectPieces(tree.root, out);
        return out;
    }

    public int getTreeLength() { return tree.treeLength(); }

    public int getLineCount() {
        int newlineCount = tree.getRoot().getNewlineCount();

        OptionalInt positionOfLastNewLineChar = tree.findNthNewlineOffset(newlineCount - 1);

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
        /*
        List<Piece> pieces = toPieceList();

        if (lineIndex < 0 || lineIndex >= lineCache.size()) {
            throw new IndexOutOfBoundsException("Line index out of bounds: " + lineIndex);
        }

        Line lineInfo = lineCache.get(lineIndex);
        if (lineInfo.length == 0) return "";

        StringBuilder lineBuilder = new StringBuilder(lineInfo.length);
        int remainingLength = lineInfo.length;
        int currentPieceIndex = lineInfo.startPieceIndex;
        int offsetInPiece = lineInfo.startOffsetInPiece;

        while (remainingLength > 0 && currentPieceIndex < pieces.size()) {
            Piece p = pieces.get(currentPieceIndex);
            String bufferContent = p.getBuffer().toString();
            int charsToRead = Math.min(remainingLength, p.getLength() - offsetInPiece);

            lineBuilder.append(bufferContent, p.getStart() + offsetInPiece, p.getStart() + offsetInPiece + charsToRead);

            remainingLength -= charsToRead;
            currentPieceIndex++;
            offsetInPiece = 0;
        }
        return lineBuilder.toString();
            */
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
            OptionalInt nthNewlinePos = tree.findNthNewlineOffset(lineIndex - 1);
            if (nthNewlinePos.isEmpty()) return Optional.empty();
            startPos = nthNewlinePos.getAsInt() + 1;
        }

        // find leaf and localOffset for startPos
        Optional<PieceTree.NodeLocation> nodeOffset = tree.translateToNodeLocation(startPos);
        if (nodeOffset.isEmpty()) return Optional.empty();
        PieceTree.PieceNode node = nodeOffset.get().node();
        int offset = nodeOffset.get().localOffset();

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


package texteditor.model;

import java.util.*;

public class PieceTable {

    private final String originalBuffer;
    private final StringBuilder addBuffer;
    private final PieceTree pieceTree;
    private final List<Line> lineCache;
    private int totalLength;

    public PieceTable(String originalText) {
        this.originalBuffer = originalText;
        this.addBuffer = new StringBuilder();
        // this.pieces = new ArrayList<>();
        this.lineCache = new ArrayList<>();
        this.pieceTree = new PieceTree();

        if (!originalText.isEmpty()) {
            Piece piece = new Piece(Piece.BufferType.ORIGINAL, 0, originalText.length());
            pieceTree.insert(0, piece);
            this.totalLength = piece.getLength();
        }

        rebuildLineCache();
    }

    public void insert(int position, String text) {
        if (text == null || text.isEmpty()) return;

        int textLength = text.length();
        addBuffer.append(text);
        Piece newPiece = new Piece(Piece.BufferType.ADD, addBuffer.length() - textLength, textLength);

        insertHelper(position, newPiece);
        totalLength += textLength;
        rebuildLineCache();
    }

    private void insertHelper(int position, Piece pieceToInsert) {
        Optional<PieceTree.NodeOffset> result = pieceTree.findNodeAndOffset(position);

        if (result.isEmpty()) {
            pieceTree.setRoot(pieceTree.createLeafNode(pieceToInsert));
            return;
        }

        RBTree.Node<Piece> node = result.get().node();
        int offset = result.get().offset();

        Piece oldPiece = node.payload;
        if (offset == 0) {
            RBTree.Node<Piece> newLeaf = pieceTree.createLeafNode(pieceToInsert);
            pieceTree.addSiblingNode(node, newLeaf, true);
            pieceTree.insertFixup(newLeaf);
        } else if (offset == oldPiece.getLength()) {
            // new piece after current leaf
            RBTree.Node<Piece> newNode = pieceTree.createLeafNode(pieceToInsert);
            pieceTree.addSiblingNode(node, newNode, false);
            pieceTree.insertFixup(newNode);
        } else {
            RBTree.Node<Piece> newNode = pieceTree.createLeafNode(pieceToInsert);
            pieceTree.splitLeafNode(node, newNode, offset);
            pieceTree.insertFixup(newNode);
        }
    }

    public void remove(int position, int length) {
        if (length <= 0 || position < 0 || position >= totalLength) return;

        if (position + length > totalLength) {
            length = totalLength - position;  // trim to valid range
        }

        removeHelper(position, length);
        totalLength -= length;

        rebuildLineCache();
    }

    private void removeHelper(int position, int removeLength) {
        if (removeLength <= 0) throw new IllegalArgumentException("Illegal remove length: " + removeLength);
        if (pieceTree.root == null) throw new IllegalStateException("Tree is empty");

        Optional<PieceTree.NodeRange> result = pieceTree.findNodeAndRange(position, removeLength);
        if (result.isEmpty()) {
            throw new IndexOutOfBoundsException("Invalid deletion range: pos=" + position + ", len=" + removeLength);
        }

        PieceTree.NodeOffset start = result.get().start();
        PieceTree.NodeOffset end = result.get().end();

        if (start.node() == end.node()) {
            RBTree.Node<Piece> leaf = start.node();
            Piece piece = leaf.payload;

            int leftLen = start.offset();
            int rightLen = piece.getLength() - end.offset();

            if (leftLen > 0 && rightLen > 0) {
                Piece leftPiece = new Piece(piece.getSource(), piece.getStart(), leftLen);
                Piece rightPiece = new Piece(piece.getSource(), piece.getStart() + end.offset(), rightLen);

                RBTree.Node<Piece> leftNode = pieceTree.createLeafNode(leftPiece);
                RBTree.Node<Piece> rightNode = pieceTree.createLeafNode(rightPiece);
                RBTree.Node<Piece> newParent = pieceTree.createInternalNode(leftNode, rightNode);

                newParent.color = leaf.color;

                pieceTree.replaceChild(leaf.parent, leaf, newParent);
                return;
            } else if (leftLen > 0) {
                Piece leftPiece = new Piece(piece.getSource(), piece.getStart(), leftLen);
                RBTree.Node<Piece> leftNode = pieceTree.createLeafNode(leftPiece);
                pieceTree.recompute(leftNode);

                leftNode.color = leaf.color;

                pieceTree.replaceChild(leaf.parent, leaf, leftNode);
                return;
            } else if (rightLen > 0) {
                Piece rightPiece = new Piece(piece.getSource(), piece.getStart() + end.offset(), rightLen);
                RBTree.Node<Piece> rightNode = pieceTree.createLeafNode(rightPiece);
                pieceTree.recompute(rightNode);

                rightNode.color = leaf.color;

                pieceTree.replaceChild(leaf.parent, leaf, rightNode);
                return;
            } else {
                pieceTree.replaceChild(leaf.parent, leaf, null);
                if (leaf.isBlack()) {
                    RBTree.Node<Piece> problemNode = pieceTree.findNodeForFixup(leaf);
                    if (problemNode != null) pieceTree.removeFixup(problemNode);
                }
                return;
            }
        }
        // TODO:  create a version that grabs the interior nodes prior to trimming start and end nodes, and removes them
        RBTree.Node<Piece> startLeaf = start.node();
        RBTree.Node<Piece> endLeaf = end.node();

        if (start.offset() < startLeaf.payload.getLength()) {
            Piece leftPiece = new Piece(startLeaf.payload.getSource(), startLeaf.payload.getStart(), start.offset());
            if (leftPiece.getLength() > 0) {
                RBTree.Node<Piece> leftNode = pieceTree.createLeafNode(leftPiece);
                pieceTree.replaceChild(startLeaf.parent, startLeaf, leftNode);
                startLeaf = leftNode;
            } else {
                pieceTree.replaceChild(startLeaf.parent, startLeaf, null);
            }

        }

        int rightLen = endLeaf.payload.getLength() - end.offset();
        if (rightLen > 0) {
            Piece rightPiece = new Piece(endLeaf.payload.getSource(), endLeaf.payload.getStart() + end.offset(), rightLen);
            RBTree.Node<Piece> rightNode = pieceTree.createLeafNode(rightPiece);
            pieceTree.replaceChild(endLeaf.parent, endLeaf, rightNode);
            endLeaf = rightNode;
        } else {
            pieceTree.replaceChild(endLeaf.parent, endLeaf, null);
        }

        RBTree.Node<Piece> returnLeaf = pieceTree.removeBetweenLeaves(startLeaf, endLeaf);
        pieceTree.bubbleRecompute(startLeaf);
        pieceTree.bubbleRecompute(endLeaf);
        if (returnLeaf.isBlack()) {
            RBTree.Node<Piece> problemNode = pieceTree.findNodeForFixup(returnLeaf);
            if (problemNode != null) pieceTree.removeFixup(problemNode);
        }
    }

    public String getText() {
        StringBuilder sb = new StringBuilder(pieceTree.treeLength());
        getTextHelper(pieceTree.root, sb);
        return sb.toString();
    }

    private void getTextHelper(RBTree.Node<Piece> node, StringBuilder stringBuilder) {
        if (node == null) {return;}
        if (node.isLeaf()) {
            String text = node.payload.getText(originalBuffer, addBuffer);
            stringBuilder.append(text);
        } else {
            getTextHelper(node.left, stringBuilder);
            getTextHelper(node.right, stringBuilder);
        }
    }

    private void collectPieces(RBTree.Node<Piece> node, List<Piece> out) {
        if (node == null) return;
        if (node.isLeaf()) out.add(node.payload);
        else {
            collectPieces(node.left, out);
            collectPieces(node.right, out);
        }
    }
    public List<Piece> toPieceList() {
        List<Piece> out = new ArrayList<>();
        collectPieces(pieceTree.root, out);
        return out;
    }

    public int getTreeLength() { return pieceTree.treeLength(); }

    private void rebuildLineCache() {
        List<Piece> pieces = toPieceList();
        lineCache.clear();
        if (pieces.isEmpty()) {
            lineCache.add(new Line(0, 0, 0));
            return;
        }

        int currentLineStartPiece = 0;
        int currentLineStartOffset = 0;
        int currentLineLength = 0;

        for (int pieceIndex = 0; pieceIndex < pieces.size(); pieceIndex++) {
            Piece piece = pieces.get(pieceIndex);
            List<Integer> lineStarts = piece.getLineStarts(originalBuffer, addBuffer);

            if (lineStarts.size() <= 1) {
                currentLineLength += piece.getLength();
            } else {
                for (int i = 1; i < lineStarts.size(); i++) {
                    int lineStartInPiece = (i == 1) ? 0 : lineStarts.get(i - 1);
                    int lineEndInPiece = lineStarts.get(i) - 1;
                    int segmentLength = lineEndInPiece - lineStartInPiece + 1;

                    currentLineLength += segmentLength;

                    lineCache.add(new Line(currentLineStartPiece, currentLineStartOffset, currentLineLength));

                    currentLineStartPiece = pieceIndex;
                    currentLineStartOffset = lineStarts.get(i);
                    currentLineLength = 0;

                }

                int lastNewlinePos = lineStarts.getLast();
                if (lastNewlinePos < piece.getLength()) {
                    currentLineLength += piece.getLength() - lastNewlinePos;
                }
            }
        }
        if (currentLineLength > 0 || lineCache.isEmpty()) {
            lineCache.add(new Line(currentLineStartPiece, currentLineStartOffset, currentLineLength));
        }
    }

    public int getLineCount() {
        return this.lineCache.size();
    }

    public int getLineLength(int lineIndex) {
        return getLine(lineIndex).length();
    }

    public boolean isLastLine(int lineIndex) {
        return lineIndex == getLineCount() - 1;
    }

    public String getLine(int lineIndex) {
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
            String bufferContent = (p.getSource() == Piece.BufferType.ORIGINAL) ? originalBuffer : addBuffer.toString();
            int charsToRead = Math.min(remainingLength, p.getLength() - offsetInPiece);

            lineBuilder.append(bufferContent, p.getStart() + offsetInPiece, p.getStart() + offsetInPiece + charsToRead);

            remainingLength -= charsToRead;
            currentPieceIndex++;
            offsetInPiece = 0;
        }
        return lineBuilder.toString();
    }
}


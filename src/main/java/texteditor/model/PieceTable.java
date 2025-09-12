package texteditor.model;

import java.util.*;

public class PieceTable {
    private class Node {
        Piece piece;
        Node left, right;
        int length;

        Node(Piece piece) {
            this.piece = piece;
            this.length = piece.getLength();
        }

        Node(Node left, Node right) {
            this.left = left;
            this.right = right;
            this.length = (left != null ? left.length : 0) + (right != null ? right.length : 0);
        }

        boolean isLeaf() {
            return piece != null;
        }
    }

    private final String originalBuffer;
    private final StringBuilder addBuffer;
    private final List<Piece> pieces;
    private Node root;
    private final List<Line> lineCache;
    private int totalLength;

    public PieceTable(String originalText) {
        this.originalBuffer = originalText;
        this.addBuffer = new StringBuilder();
        this.pieces = new ArrayList<>();
        this.lineCache = new ArrayList<>();

        if (!originalText.isEmpty() && originalText != null) {
            Piece piece = new Piece(Piece.BufferType.ORIGINAL, 0, originalText.length());
            this.root = new Node(piece);
            pieces.add(piece);
            this.totalLength = piece.getLength();
        }

        rebuildLineCache();
    }

    private void collectText(Node node, StringBuilder stringBuilder) {
        if (node == null) return;
        if (node.isLeaf()) {
            stringBuilder.append(node.piece.getText(originalBuffer, addBuffer));
        } else {
            collectText(node.left, stringBuilder);
            collectText(node.right, stringBuilder);
        }
    }

    private Node buildTree(List<Node> nodes) {
        if (nodes.isEmpty()) return null;
        while (nodes.size() > 1) {
            List<Node> next = new ArrayList<>();
            for (int i = 0; i < nodes.size(); i+=2) {
                if (i + 1 < nodes.size()) {
                    next.add(new Node(nodes.get(i), nodes.get(i + 1)));
                } else {
                    next.add(nodes.get(i));
                }
            }
            nodes = next;
        }
        return nodes.get(0);
    }

    public String getText() {
        StringBuilder result = new StringBuilder();
        for (Piece piece : pieces) {
            result.append(piece.getText(originalBuffer, addBuffer));
        }
        return result.toString();
    }

    public int getLength() {
        return this.totalLength;
    }

    public record TextLocation(Piece piece, int offsetInPiece, int pieceIndex) {}

    public TextLocation findLocation(int position) {
        return findLocationRecursive(root, position);
    }

    public TextLocation findLocationRecursive(Node node, int position) {
        if (node.isLeaf()) {
            return new TextLocation(node.piece, position, -1);
        }

        int leftLen = (node.left != null) ? node.left.length : 0;

        if (position < leftLen) {
            return findLocationRecursive(node.left, position);
        } else {
            return findLocationRecursive(node.right, position - leftLen);
        }
    }

    public TextLocation findLocationAt(int position) {
        int currentLength = 0;
        for (int i = 0; i < pieces.size(); i++) {
            currentLength += pieces.get(i).getLength();
            if (currentLength >= position) {
                int localIndex = pieces.get(i).getLength() - (currentLength - position);
                return new TextLocation(pieces.get(i), localIndex, i);
            }
        }
        return null;
    }

    public String getTextSpan(int position, int length) {
        return "";
    }

    private Node insert(Node node, int position, String text) {
        if (node.isLeaf()) {
            Piece oldPiece = node.piece;
            int offset = position;

            // Case 1: insert at start
            if (offset == 0) {
                Node newLeaf = new Node(new Piece(Piece.BufferType.ADD,
                        addBuffer.length(), text.length()));
                addBuffer.append(text);
                return new Node(newLeaf, node);
            }

            // Case 2: insert at end
            if (offset == oldPiece.getLength()) {
                Node newLeaf = new Node(new Piece(Piece.BufferType.ADD,
                        addBuffer.length(), text.length()));
                addBuffer.append(text);
                return new Node(node, newLeaf);
            }

            // Case 3: split in middle
            Piece leftPart = new Piece(oldPiece.getSource(), oldPiece.getStart(), offset);
            Piece rightPart = new Piece(oldPiece.getSource(),
                    oldPiece.getStart() + offset,
                    oldPiece.getLength() - offset);
            Node leftLeaf = new Node(leftPart);
            Node rightLeaf = new Node(rightPart);

            Node newLeaf = new Node(new Piece(Piece.BufferType.ADD,
                    addBuffer.length(), text.length()));
            addBuffer.append(text);

            // Build subtree: (leftLeaf + newLeaf + rightLeaf)
            return new Node(leftLeaf, new Node(newLeaf, rightLeaf));
        }

        // Internal node: decide whether to go left or right
        int leftLen = (node.left != null) ? node.left.length : 0;
        if (position < leftLen) {
            Node newLeft = insert(node.left, position, text);
            return new Node(newLeft, node.right);
        } else {
            Node newRight = insert(node.right, position - leftLen, text);
            return new Node(node.left, newRight);
        }
    }

    public void insertRecursive(int position, String text) {
        if (text == null || text.isEmpty()) return;
        root = insert(root, position, text);
        totalLength += text.length();
    }


    public void insert(int position, String text) {
        if (text == null || text.isEmpty()) return;

        int textLength = text.length();
        addBuffer.append(text);
        Piece newPiece = new Piece(Piece.BufferType.ADD, addBuffer.length() - textLength, textLength);

        if (pieces.isEmpty()) {
            pieces.add(newPiece);
            totalLength += textLength;
            rebuildLineCache();
            return;
        }

        TextLocation insertionPoint = findLocationAt(position);
        if (insertionPoint == null) return;

        Piece pieceToSplit = insertionPoint.piece;
        int splitOffset = insertionPoint.offsetInPiece;
        int targetIndex = insertionPoint.pieceIndex;

        if (splitOffset == 0) {
            pieces.add(targetIndex, newPiece);
        }
        else if (splitOffset == pieceToSplit.getLength()) {
            pieces.add(targetIndex + 1, newPiece);
        } else {
            Piece leftPart = new Piece(pieceToSplit.getSource(), pieceToSplit.getStart(), splitOffset);
            Piece rightPart = new Piece(pieceToSplit.getSource(), pieceToSplit.getStart() + splitOffset, pieceToSplit.getLength() - splitOffset);

            pieces.remove(targetIndex);
            pieces.add(targetIndex, leftPart);
            pieces.add(targetIndex + 1, newPiece);
            pieces.add(targetIndex + 2, rightPart);

        }
        this.totalLength += text.length();
        rebuildLineCache();
    }

    public void remove(int position, int length) {
        if (length <= 0 || position < 0 || position + length > this.totalLength) return;

        TextLocation startLocation = findLocationAt(position);
        TextLocation endLocation = findLocationAt(position + length);

        if (startLocation.pieceIndex() == endLocation.pieceIndex()) {
            Piece pieceToModify = startLocation.piece();
            int pieceIndex = startLocation.pieceIndex();

            int startOffset = startLocation.offsetInPiece();
            int endOffset = endLocation.offsetInPiece();

            pieces.remove(pieceIndex);

            Piece leftPart = (startOffset > 0)
                    ? new Piece(pieceToModify.getSource(), pieceToModify.getStart(), startOffset)
                    : null;

            Piece rightPart = (endOffset < pieceToModify.getLength())
                    ? new Piece(pieceToModify.getSource(), pieceToModify.getStart() + endOffset,
                    pieceToModify.getLength() - endOffset)
                    : null;

            int currentIndex = pieceIndex;
            if (leftPart != null) pieces.add(currentIndex++, leftPart);
            if (rightPart != null) pieces.add(currentIndex, rightPart);
        }
        else {
            Piece firstPiece = startLocation.piece();
            int firstPieceIndex = startLocation.pieceIndex();
            int firstPieceStartOffset = startLocation.offsetInPiece();

            Piece lastPiece = endLocation.piece();
            int lastPieceIndex = endLocation.pieceIndex();
            int lastPieceEndOffset = endLocation.offsetInPiece();

            // remove all middle pieces that are fully consumed when deleting
            for (int i = lastPieceIndex - 1; i > firstPieceIndex; i--) {
                pieces.remove(i);
            }

            if (lastPieceEndOffset < lastPiece.getLength()) {
                Piece rightPart = new Piece(lastPiece.getSource(), lastPiece.getStart() + lastPieceEndOffset,
                        lastPiece.getLength() - lastPieceEndOffset);
                pieces.set(firstPieceIndex + 1, rightPart);
            } else {
                pieces.remove(firstPieceIndex + 1);
            }

            if (firstPieceStartOffset > 0) {
                Piece leftPart = new Piece(firstPiece.getSource(), firstPiece.getStart(), firstPieceStartOffset);
                pieces.set(firstPieceIndex, leftPart);
            } else {
                pieces.remove(firstPieceIndex);
            }
        }
        totalLength -= length;
        rebuildLineCache();
    }

    private void rebuildLineCache() {
        lineCache.clear();
        if (pieces.isEmpty()) {
            lineCache.add(new Line(0,0,0));
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

                int lastNewlinePos = lineStarts.get(lineStarts.size() - 1);
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

    public static void main(String[] args) {
        PieceTable testTable = new PieceTable("Does this work ");
        String text = testTable.getText();
        System.out.println(text);

        testTable.insert(10, "really ");
        String newtext = testTable.getText();
        System.out.println(newtext);

        testTable.insert(21, " wow ");
        String newtext2 = testTable.getText();
        System.out.println(newtext2);

        testTable.insert(10, "thing \n");
        String newtext3 = testTable.getText();
        System.out.println(newtext3);

        testTable.remove(10, 5);
        System.out.println(testTable.getText());

        System.out.println(testTable.getLength());

        System.out.println(testTable.getLineCount());

        System.out.println(testTable.getLine(0));


    }
}


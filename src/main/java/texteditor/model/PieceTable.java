package texteditor.model;

import java.util.*;

public class PieceTable {
    private final OriginalBuffer originalBuffer;
    private final AddBuffer addBuffer;
    final PieceTree tree;
    private int length;
    private int lines;

    public PieceTable(String originalText) {
        this.originalBuffer = new OriginalBuffer(originalText);
        this.addBuffer = new AddBuffer();
        this.tree = new PieceTree();

        if (!originalText.isEmpty()) {
            Piece piece = new Piece(originalBuffer, 0, originalText.length());
            tree.insertHelper(0, piece);
        }
        this.length = tree.length();
    }

    public void remove(int globalOffsetInclusive, int length) {
        if (length <= 0 || globalOffsetInclusive < 0 || globalOffsetInclusive + length > this.length) return;

        if (tree.getRoot() == tree.NIL) return;

        tree.removeHelper(globalOffsetInclusive, length);
        tree.calcLength();
        this.length =  tree.length();
        this.lines = tree.lines;

    }

    public int length(){
        return this.length;
    }

    // TODO: UPDATE LOGIC
    private void getTextHelper(PieceTree.PieceNode node, StringBuilder stringBuilder) {
        if (node == tree.NIL) {
            return;
        }

        getTextHelper(node.left, stringBuilder);
        stringBuilder.append(node.payload.getText());
        getTextHelper(node.right, stringBuilder);
    }

    public String getText() {
        StringBuilder sb = new StringBuilder(tree.length());
        tree.traverseInOrder(pieceNode -> sb.append(pieceNode.payload.getText()));
        String text = sb.toString();
        return text;
    }

    public void insert(int globalOffset, String text) {
        if (text == null || text.isEmpty()) return;

        int textLength = text.length();
        addBuffer.append(text);

        Piece newPiece = new Piece(addBuffer, addBuffer.length() - textLength, textLength);
        tree.insertHelper(globalOffset, newPiece);
        assert tree.getRoot().parent == tree.NIL;
        tree.calcLength();
        this.length =  tree.length();
        this.lines = tree.lines;
    }


    public OptionalInt getGlobalOffsetOfLine(int lineNumber) {
        int lineIndex = lineNumber - 1; // convert to 0-based
        if (tree.getRoot() == null || lineIndex < 0 || lineIndex >= tree.getRoot().totalLines()) return OptionalInt.empty();

        PieceTree.PieceNode node = tree.getRoot();
        int offset = 0;

        // Traverse down to leaf
        while (!tree.isLeaf(node)) {
            int leftLines = (node.left != tree.NIL) ? node.left.lftLineCount + node.left.totalLines() : 0;
            if (lineIndex < leftLines) {
                node = node.left;
            } else {
                int leftLen = (node.left != tree.NIL) ? node.left.lftSize + node.left.length() : 0;
                offset += leftLen;
                lineIndex -= leftLines;
                node = node.right;
            }
        }

        // Now node is a leaf, scan only this leaf
        Piece piece = node.payload;
        for (int i = 0; i < node.length(); i++) {
            if (piece.getChar(i) == '\n') {
                if (lineIndex == 0) return OptionalInt.of(offset + i);
                lineIndex--;
            }
        }

        // If the line is beyond this leaf (shouldn’t happen if metadata is correct)
        return OptionalInt.empty();
    }


    public int getLineCount() {
        if (tree.getRoot() == null) return 0;  // empty document
        int newlineCount = tree.getRoot().totalLines(); // total number of '\n'

        if (newlineCount == 0) return 1;

        int lastCharIndex = tree.length() - 1;
        int lastNewlineOffset = getGlobalOffsetOfLine(newlineCount).orElse(-1);

        return (lastNewlineOffset == lastCharIndex) ? newlineCount : newlineCount + 1;
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

        int totalLines = tree.getRoot().totalLines() + (tree.length() > 0 ? 1 : 0);
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
        PieceTree.NodeLocation nodeOffset = tree.getNodeLocation(startPos);
        PieceTree.PieceNode node = nodeOffset.node();
        int offset = nodeOffset.localOffset();

        StringBuilder sb = new StringBuilder();
        PieceTree.PieceNode cur = node;
        int curOffset = offset;

        while (cur != tree.NIL) {
            for (int i = curOffset; i < cur.length(); i++) {
                char c = cur.payload.getChar(i);
                sb.append(c);
                if (c == '\n') {
                    return Optional.of(sb.toString());
                }
            }
            cur = tree.nextNode(cur);
            curOffset = 0;
        }
        return Optional.of(sb.toString());
    }
}


package texteditor.model;

import java.util.*;

public class PieceTable extends RBTree<PieceTable.PieceNode, Piece> {

    private final OriginalBuffer originalBuffer;
    private final AddBuffer addBuffer;
    private int length;
    private int lines;

    public static class PieceNode extends Node<PieceTable.PieceNode, Piece> {
        private int lftLineCount;

        PieceNode(Piece piece) {
            super(piece);
        }

        PieceNode(Piece piece, PieceTable.PieceNode left, PieceTable.PieceNode right) {
            super(piece, left, right);
        }

        private int size() {
            int lfSize = (this.left != null && this.left.payload != null) ? this.left.size() : 0;
            this.lftSize = lfSize;
            int rtSize = (this.right != null && this.right.payload != null) ? this.right.size() : 0;
            return lfSize + this.length() + rtSize;
        }

        private int totalLines() {
            int lfLineCount = (this.left != null && this.left.payload != null) ? this.left.totalLines() : 0;
            this.lftLineCount = lfLineCount;
            int rtLineCount = (this.right != null && this.right.payload != null) ? this.right.totalLines() : 0;
            return lfLineCount + this.lines() + rtLineCount;
        }

        private int length() {
            return (payload != null) ? payload.getLength() : 0;
        }

        private int lines() {
            return (payload != null) ? payload.getLineCount() : 0;
        }

    }

    public PieceTable(String originalText) {
        super();

        this.originalBuffer = new OriginalBuffer(originalText);
        this.addBuffer = new AddBuffer();

        if (!originalText.isEmpty()) {
            Piece piece = new Piece(originalBuffer, 0, originalText.length());
            insertHelper(0, piece);
        }
        this.length = this.getRoot().size();
    }

    @Override
    public int length() {
        if (this.length > 0) {
            return this.length;
        } else if (this.getRoot() == this.NIL) {
            return 0;
        } else if (this.getRoot() != this.NIL){
            calcLength();
            return this.length;
        }
        return 0;
    }


    @Override
    public PieceNode createSentinel() {
        PieceNode sentinel = new PieceNode(null);
        sentinel.left = sentinel.right = sentinel.parent = sentinel;
        sentinel.color = Color.BLACK;
        sentinel.lftSize = 0;
        sentinel.lftLineCount = 0;
        return sentinel;
    }

    public void calcLength() {
        this.length = (root != NIL) ? getRoot().size() : 0;
        this.lines = (root != NIL) ? getRoot().totalLines() : 0;
    }



    @Override
    protected PieceNode createNode(Piece piece) {
        PieceNode node = new PieceNode(piece, this.NIL, this.NIL);
        node.parent = this.NIL;
        bubbleRecompute(node);
        return node;
    }

    @Override
    protected boolean isLeaf(PieceNode node) {
        return (node.left == this.NIL && node.right == this.NIL);
    }

    @Override
    protected void recompute(PieceNode node) {
        if (node == this.NIL) return;

        node.lftSize = (node.left != null && node.left.payload != null) ? node.left.size() : 0;
        node.lftLineCount = (node.left != null && node.left.payload != null) ? node.left.totalLines() : 0;
    }

    protected void replaceNode(PieceNode y, PieceNode node, PieceNode x) {
        if (x != this.NIL) {
            if (y.parent == node) x.parent = y;
            else x.parent = y.parent;
        }

        bubbleRecompute(x); // update subtree metadata first

        y.left = node.left;
        y.right = node.right;
        y.parent = node.parent;
        y.color = node.color;

        if (node == getRoot()) setRoot(y);
        else if (node == node.parent.left) node.parent.left = y;
        else node.parent.right = y;

        if (y.left != this.NIL) y.left.parent = y;
        if (y.right != this.NIL) y.right.parent = y;

        y.lftSize = node.lftSize;
        y.lftLineCount = node.lftLineCount;
        bubbleRecompute(y);
    }

    private void deleteNodeTail(PieceNode node, int newExclusiveEnd) {
        Piece piece = node.payload;
        node.payload = new Piece(piece.getBuffer(), piece.getStart(), newExclusiveEnd);
        recompute(node);
    }

    private void deleteNodeHead(PieceNode node, int newInclusiveStart) {
        Piece piece = node.payload;
        node.payload = new Piece(piece.getBuffer(), piece.getStart() + newInclusiveStart, piece.getLength() - newInclusiveStart);
        recompute(node);
    }

    private void deleteNodeCharsInRange(PieceNode node, int inclusiveStart, int exclusiveEnd) {
        if (inclusiveStart == 0 || exclusiveEnd == node.length())
            throw new IllegalArgumentException("Range exceeds the middle of the node");

        Piece piece = node.payload;
        int bufferStartOffset = piece.getStart();
        int bufferEndOffset = piece.getEnd();

        Piece leftPiece = new Piece(piece.getBuffer(), bufferStartOffset, inclusiveStart);
        Piece rightPiece = new Piece(piece.getBuffer(), bufferStartOffset + exclusiveEnd, piece.getLength() - exclusiveEnd);

        node.payload = leftPiece;
        recompute(node);

        addToRight(node, rightPiece);
    }

    private void addPieceInsideNode(PieceNode nodeToSplit, Piece newPiece, int localOffset) {
        Piece oldPiece = nodeToSplit.payload;
        int oldLength = oldPiece.getLength();


        Piece rightPiece = new Piece(oldPiece.getBuffer(), oldPiece.getStart() + localOffset, oldLength - localOffset);
        deleteNodeTail(nodeToSplit, localOffset);
        addToRight(nodeToSplit, rightPiece);

        addToRight(nodeToSplit, newPiece);
        bubbleRecompute(nodeToSplit);
    }

    record NodeLocation(PieceNode node, int localOffset, int nodeStartOffset) {
    }

    NodeLocation getNodeLocation(int globalOffset) {
        if (this.root == null) throw new IllegalStateException("tree is empty");

        if (globalOffset < 0 || globalOffset > this.length()) {
            throw new IndexOutOfBoundsException(
                    "globalOffset " + globalOffset + " out of bounds [0, " + this.length() + ")"
            );
        }

        PieceNode node = this.getRoot();
        int startOffset = globalOffset;

        while (node != this.NIL) {
            int leftLen = node.lftSize;

            if (globalOffset < leftLen) {
                node = node.left;
            } else if (leftLen + node.length() >= globalOffset) {
                startOffset += node.length();
                return new NodeLocation(node, globalOffset - leftLen, node.payload.getStart());
            } else {
                globalOffset -= leftLen + node.length();
                startOffset += leftLen + node.length();
                node = node.right;
            }
        }
        return null;
    }

    // TODO: CORRECT REMOVE LOGIC
    @Override
    protected void deleteNode(PieceNode node) {
        if (node == this.NIL) return;

        PieceNode x = this.NIL;
        PieceNode y = this.NIL;

        // determine which node to actually remove (splice node)
        if (node.left == this.NIL || node.right == this.NIL) {
            y = node;
        } else {
            y = leftmost(node.right);
        } // in-order successor

        x = (y.left != this.NIL) ? y.left : y.right; // get replacement, which will be child of y, if y is node

        // handle root deletion case
        if (y == getRoot()) {
            setRoot(x);
            detach(node);
            return;
        }

        // splice out y and update parent pointers
        if (y == y.parent.left) y.parent.left = x;
        else y.parent.right = x;
        if (x != this.NIL) x.parent = y.parent;

        // if y was the in-order successor and not the actual node, replace the node with y
        if (y != node) {
            replaceNode(y, node, x);       // Move successor y into node's position
        }

        PieceNode p = (x != NIL ? x.parent : y.parent);
        while (p != NIL) {
            recompute(p);
            p = p.parent;
        }

        if (y.color == Color.BLACK) {
            removeFixup(x);         // RB-DELETE-FIXUP
        }
        detach(node); // Remove references
    }

    private void deleteNodes(List<PieceNode> nodes) {
        for (PieceNode node : nodes) {
            deleteNode(node);
        }
    }

    private void removeHelper(int globalOffsetInclusive, int removeLength) {

        NodeLocation rmStart = getNodeLocation(globalOffsetInclusive);
        NodeLocation rmEnd = getNodeLocation(globalOffsetInclusive + removeLength);

        PieceNode rmStartNode = rmStart.node();
        PieceNode rmEndNode = rmEnd.node();

        if (rmStartNode == rmEndNode) {
            PieceNode node = rmStartNode;
            Piece piece = node.payload;

            int startOffsetInclusive = rmStart.localOffset();
            int endOffsetExclusive = rmEnd.localOffset();

            // delete entire node
            if (startOffsetInclusive == 0 && removeLength == piece.getLength()) {
                deleteNode(node);
                bubbleRecompute(node.parent);
                return;
            } else if (startOffsetInclusive > 0 && removeLength + startOffsetInclusive == piece.getLength()) {
                deleteNodeTail(node, startOffsetInclusive);
            } else if (startOffsetInclusive == 0 && removeLength < piece.getLength()) {
                deleteNodeHead(node, endOffsetExclusive);
            } else if (startOffsetInclusive > 0 && removeLength + startOffsetInclusive  < piece.getLength()) {
                deleteNodeCharsInRange(node, startOffsetInclusive, endOffsetExclusive);
            }
            return;
        }

        // TODO:  create a version that grabs the interior nodes prior to trimming startLocation and endLocation nodes, and removes them
        List<PieceNode> nodesToDelete = new ArrayList<>();

        // delete
        if (rmStart.localOffset() > 0) {
            deleteNodeTail(rmStartNode, rmStart.localOffset());
        } else {
            nodesToDelete.add(rmStartNode);
        }

        if (rmEnd.localOffset() <= rmEndNode.length() - 1) {
            deleteNodeHead(rmEndNode, rmEnd.localOffset());
        } else {
            nodesToDelete.add(rmEndNode);
        }

        PieceNode nextNode = nextNode(rmStartNode);
        while (nextNode != this.NIL && nextNode != rmEndNode) {
            nodesToDelete.add(nextNode);
            nextNode = nextNode(nextNode);
        }

        deleteNodes(nodesToDelete);
    }


    public void remove(int globalOffsetInclusive, int length) {
        if (length <= 0 || globalOffsetInclusive < 0 || globalOffsetInclusive + length > this.length) return;

        if (this.getRoot() == this.NIL) return;

        removeHelper(globalOffsetInclusive, length);
        calcLength();
    }

    // TODO: UPDATE LOGIC
    private void getTextHelper(PieceNode node, StringBuilder stringBuilder) {
        if (node == this.NIL) {
            return;
        }

        getTextHelper(node.left, stringBuilder);
        stringBuilder.append(node.payload.getText());
        getTextHelper(node.right, stringBuilder);
    }

    public String getText() {
        if (getRoot() == this.NIL) return "";
        StringBuilder sb = new StringBuilder(length());
        getTextHelper(this.root, sb);
        return sb.toString();
    }

    public void insert(int globalOffset, String text) {
        if (text == null || text.isEmpty()) return;

        int textLength = text.length();
        addBuffer.append(text);

        Piece newPiece = new Piece(addBuffer, addBuffer.length() - textLength, textLength);
        insertHelper(globalOffset, newPiece);
        assert root.parent == NIL;
        calcLength();
    }

    private void insertHelper(int globalOffset, Piece pieceToInsert) {
        if (this.getRoot() == NIL) {
            if (globalOffset != 0) {
                throw new IllegalArgumentException(("Can only insert at position 0 when tree is empty"));
            }
            setRoot(this.createNode(pieceToInsert));
            return;
        }

        if (globalOffset == length()) {
            PieceNode lastLeaf = this.lastLeaf();
            addToRight(lastLeaf, pieceToInsert);
            return;
        }

        NodeLocation result = this.getNodeLocation(globalOffset);
        PieceNode node = result.node();
        int localOffset = result.localOffset();

        if (localOffset == 0) {
            addToLeft(pieceToInsert, node);
        } else if (localOffset > 0 && localOffset < node.length() - 1) {
            addPieceInsideNode(node, pieceToInsert, localOffset);
        } else {
            addToRight(node, pieceToInsert);
        }
    }

    public OptionalInt getGlobalOffsetOfLine(int lineNumber) {
        int lineIndex = lineNumber - 1; // convert to 0-based
        if (root == null || lineIndex < 0 || lineIndex >= root.totalLines()) return OptionalInt.empty();

        PieceNode node = root;
        int offset = 0;

        // Traverse down to leaf
        while (!isLeaf(node)) {
            int leftLines = (node.left != NIL) ? node.left.lftLineCount + node.left.totalLines() : 0;
            if (lineIndex < leftLines) {
                node = node.left;
            } else {
                int leftLen = (node.left != NIL) ? node.left.lftSize + node.left.length() : 0;
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
        if (root == null) return 0;  // empty document
        int newlineCount = root.totalLines(); // total number of '\n'

        if (newlineCount == 0) return 1;

        int lastCharIndex = length() - 1;
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
        if (this.getRoot() == null) return Optional.empty();

        int totalLines = this.getRoot().totalLines() + (this.length() > 0 ? 1 : 0);
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
        NodeLocation nodeOffset = this.getNodeLocation(startPos);
        PieceNode node = nodeOffset.node();
        int offset = nodeOffset.localOffset();

        StringBuilder sb = new StringBuilder();
        PieceNode cur = node;
        int curOffset = offset;

        while (cur != NIL) {
            for (int i = curOffset; i < cur.length(); i++) {
                char c = cur.payload.getChar(i);
                sb.append(c);
                if (c == '\n') {
                    return Optional.of(sb.toString());
                }
            }
            cur = this.nextNode(cur);
            curOffset = 0;
        }
        return Optional.of(sb.toString());
    }
}


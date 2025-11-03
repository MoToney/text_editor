package texteditor.model;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

public class PieceTree extends RBTree<PieceTree.PieceNode, Piece> {
    private int length;
    int lines;

    record Metrics(int length, int lines) {
    }

    ;

    public static class PieceNode extends Node<PieceNode, Piece> {
        int lftLineCount;

        PieceNode(Piece piece) {
            super(piece);
        }

        PieceNode(Piece piece, PieceNode left, PieceNode right) {
            super(piece, left, right);
        }

        int size() {
            int lfSize = (this.left != null && this.left.payload != null) ? this.left.size() : 0;
            this.lftSize = lfSize;
            int rtSize = (this.right != null && this.right.payload != null) ? this.right.size() : 0;
            return lfSize + this.length() + rtSize;
        }

        int totalLines() {
            int lfLineCount = (this.left != null && this.left.payload != null) ? this.left.totalLines() : 0;
            this.lftLineCount = lfLineCount;
            int rtLineCount = (this.right != null && this.right.payload != null) ? this.right.totalLines() : 0;
            return lfLineCount + this.lines() + rtLineCount;
        }

        int length() {
            return (payload != null) ? payload.getLength() : 0;
        }

        private int lines() {
            return (payload != null) ? payload.getLineCount() : 0;
        }

    }

    public PieceTree() {
        super();
        this.length = 0;
        this.lines = 0;
    }

    private Metrics computeMetrics(PieceNode node) {
        if (node == NIL) return new Metrics(0, 0);

        Metrics left = computeMetrics(node.left);
        Metrics right = computeMetrics(node.right);

        node.lftSize = left.length();
        node.lftLineCount = left.lines();

        int totalLen = left.length() + node.length() + right.length();
        int totalLines = left.lines() + node.lines() + right.lines();
        return new Metrics(totalLen, totalLines);
    }


    public void updateTreeMetrics() {
        Metrics metrics = computeMetrics(this.getRoot());
        this.length = metrics.length();
        this.lines = metrics.lines();
    }

    @Override
    public int length() {
        if (this.length > 0) {
            return this.length;
        } else if (this.getRoot() == this.NIL) {
            return 0;
        } else if (this.getRoot() != this.NIL) {
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

    void deleteNodeTail(PieceNode node, int newExclusiveEnd) {
        Piece piece = node.payload;
        node.payload = new Piece(piece.getBuffer(), piece.getStart(), newExclusiveEnd);
        recompute(node);
    }

    void deleteNodeHead(PieceNode node, int newInclusiveStart) {
        Piece piece = node.payload;
        node.payload = new Piece(piece.getBuffer(), piece.getStart() + newInclusiveStart, piece.getLength() - newInclusiveStart);
        recompute(node);
    }

    void deleteNodeCharsInRange(PieceNode node, int inclusiveStart, int exclusiveEnd) {
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
            throw new IllegalArgumentException("Illegal global offset");
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

    void removeHelper(int globalOffsetInclusive, int removeLength) {

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
            } else if (startOffsetInclusive > 0 && removeLength + startOffsetInclusive < piece.getLength()) {
                deleteNodeCharsInRange(node, startOffsetInclusive, endOffsetExclusive);
            }
            calcLength();
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

    void insertHelper(int globalOffset, Piece pieceToInsert) {
        if (this.getRoot() == NIL) {
            if (globalOffset != 0) {
                throw new IllegalArgumentException(("Can only insert at position 0 when tree is empty"));
            }
            setRoot(this.createNode(pieceToInsert));
            calcLength();
            return;
        }

        if (globalOffset == length()) {
            PieceNode lastLeaf = this.lastLeaf();
            addToRight(lastLeaf, pieceToInsert);
            calcLength();
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
        calcLength();
    }

    public void traverseInOrder(Consumer<PieceNode> action) {
        traverseHelper(root, action);
    }

    public void traverseHelper(PieceNode node, Consumer<PieceNode> action) {
        if (node == this.NIL) return;

        traverseHelper(node.left, action);
        action.accept(node);
        traverseHelper(node.right, action);
    }
}
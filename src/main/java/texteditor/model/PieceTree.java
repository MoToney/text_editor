package texteditor.model;

import java.util.ArrayList;
import java.util.List;

public class PieceTree {

    private static class Node {
        Piece piece;
        Node left, right;
        int length;

        Node(Piece piece) {
            this.piece = piece;
            this.left = this.right = null;
            recalc();
        }

        Node(Node left, Node right) {
            this.piece = null;
            this.left = left;
            this.right = right;
            recalc();
        }

        boolean isLeaf() {
            return piece != null;
        }

        void recalc() {
            if (isLeaf()) {
                length = piece.getLength();
            } else {
                length = (left != null ? left.length : 0) + (right != null ? right.length : 0);
            }
        }
    }

    private Node root;

    public PieceTree() {
        this.root = null;
    }

    public PieceTree(Piece initial) {
        if (initial != null) this.root = new Node(initial);
    }

    public int length() {
        return (root != null) ? root.length : 0;
    }

    private void collectText(Node node, StringBuilder stringBuilder, String originalBuffer, StringBuilder addBuffer) {
        if (node == null) return;
        if (node.isLeaf()) {
            stringBuilder.append(node.piece.getText(originalBuffer, addBuffer));
        } else {
            collectText(node.left, stringBuilder, originalBuffer, addBuffer);
            collectText(node.right, stringBuilder, originalBuffer, addBuffer);
        }
    }

    public String getText(String originalBuffer, StringBuilder addBuffer) {
        StringBuilder sb = new StringBuilder(length());
        collectText(root, sb, originalBuffer, addBuffer);
        return sb.toString();
    }

    public PieceTable.TextLocation findLocation(int position) {
        if (root == null) return null;
        if (position < 0) position = 0;
        if (position > root.length) position = root.length;
        return findLocationRecursive(root, position);
    }

    public PieceTable.TextLocation findLocationRecursive(Node node, int position) {
        if (node.isLeaf()) {
            return new PieceTable.TextLocation(node.piece, position, -1);
        }

        int leftLen = (node.left != null) ? node.left.length : 0;

        if (position < leftLen) {
            return findLocationRecursive(node.left, position);
        } else {
            return findLocationRecursive(node.right, position - leftLen);
        }
    }

    public void insert(int position, Piece pieceToInsert) {
        if (pieceToInsert == null || pieceToInsert.getLength() == 0) return; // nothing to insert

        // clamp position to valid range
        if (position < 0) position = 0;
        int treeLength = (root != null) ? root.length : 0;
        if (position > treeLength) position = treeLength;

        if (root == null) {
            root = new Node(pieceToInsert);
        } else {
            root = insertRecursive(root, position, pieceToInsert);
        }
    }

    private Node insertRecursive(Node node, int position, Piece pieceToInsert) {
        if (node.isLeaf()) {
            Piece old = node.piece;
            int oldLen = old.getLength();
            int offset = Math.max(0, Math.min(position, oldLen));


            if (offset == 0) {
// new piece before current leaf
                return new Node(new Node(pieceToInsert), node);
            }
            if (offset == oldLen) {
// new piece after current leaf
                return new Node(node, new Node(pieceToInsert));
            }


// split existing leaf into left + right, and insert new piece in the middle
            Piece leftPiece = new Piece(old.getSource(), old.getStart(), offset);
            Piece rightPiece = new Piece(old.getSource(), old.getStart() + offset, oldLen - offset);
            Node leftLeaf = new Node(leftPiece);
            Node rightLeaf = new Node(rightPiece);
            Node newLeaf = new Node(pieceToInsert);


// (leftLeaf, (newLeaf, rightLeaf)) - keeps the insertion local
            return new Node(leftLeaf, new Node(newLeaf, rightLeaf));
        }


        int leftLen = (node.left != null) ? node.left.length : 0;
        if (position < leftLen) {
            Node newLeft = insertRecursive(node.left, position, pieceToInsert);
            return new Node(newLeft, node.right);
        } else {
            Node newRight = insertRecursive(node.right, position - leftLen, pieceToInsert);
            return new Node(node.left, newRight);
        }
    }

    public List<Piece> toPieceList() {
        List<Piece> out = new ArrayList<>();
        collectPieces(root, out);
        return out;
    }

    private void collectPieces(Node node, List<Piece> out) {
        if (node == null) return;
        if (node.isLeaf()) out.add(node.piece);
        else {
            collectPieces(node.left, out);
            collectPieces(node.right, out);
        }
    }

    private Node removeRecursive(Node node, int position, int removeLength) {
        if (node == null || removeLength <= 0) return node;


        if (node.isLeaf()) {
            int pieceLen = node.piece.getLength();
            int start = Math.max(0, Math.min(position, pieceLen));
            int end = Math.max(0, Math.min(position + removeLength, pieceLen));
            if (start >= end) return node; // nothing to remove in this leaf


            int leftLen = start;
            int rightLen = pieceLen - end;


            if (leftLen > 0 && rightLen > 0) {
                Piece leftPiece = new Piece(node.piece.getSource(), node.piece.getStart(), leftLen);
                Piece rightPiece = new Piece(node.piece.getSource(), node.piece.getStart() + end, rightLen);
                return new Node(new Node(leftPiece), new Node(rightPiece));
            } else if (leftLen > 0) {
                Piece leftPiece = new Piece(node.piece.getSource(), node.piece.getStart(), leftLen);
                return new Node(leftPiece);                  // <-- leaf node
            } else if (rightLen > 0) {
                Piece rightPiece = new Piece(node.piece.getSource(), node.piece.getStart() + end, rightLen);
                return new Node(rightPiece);                 // <-- leaf node
            } else {
                // entire piece deleted
                return null;
            }
        }


        int leftSubLen = (node.left != null) ? node.left.length : 0;


        if (position + removeLength <= leftSubLen) {
// deletion entirely in left subtree
            Node newLeft = removeRecursive(node.left, position, removeLength);
            return combine(newLeft, node.right);
        } else if (position >= leftSubLen) {
// entirely in right subtree
            Node newRight = removeRecursive(node.right, position - leftSubLen, removeLength);
            return combine(node.left, newRight);
        } else {
// spans both sides
            int removeFromLeft = leftSubLen - position;
            Node newLeft = removeRecursive(node.left, position, removeFromLeft);
            int remaining = removeLength - removeFromLeft;
            Node newRight = removeRecursive(node.right, 0, remaining);
            return combine(newLeft, newRight);
        }
    }

    public void remove(int position, int removeLength) {
        if (removeLength <= 0 || root == null) return;

        int treeLength = root.length;
        if (position < 0) position = 0;
        if (position >= treeLength) return; // nothing to remove
        if (position + removeLength > treeLength) {
            removeLength = treeLength - position; // trim to valid range
        }

        root = removeRecursive(root, position, removeLength);
    }


    private Node combine(Node left, Node right) {
        if (left == null) return right;
        if (right == null) return left;
        return new Node(left, right);
    }


}

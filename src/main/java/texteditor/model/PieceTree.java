package texteditor.model;

import java.util.ArrayList;
import java.util.List;

public class PieceTree {

    private enum Color {RED, BLACK}

    private static class Node {
        Piece piece;
        Node left, right, parent;
        int length;
        Color color;

        Node(Piece piece) {
            this.piece = piece;
            this.left = this.right = this.parent = null;
            this.color = Color.RED;

            recalc();
        }

        Node(Node left, Node right) {
            this.piece = null;
            this.left = left;
            this.right = right;
            this.parent = null;
            this.color = Color.RED;

            if (left != null) left.parent = this;
            if (right != null) right.parent = this;

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

        boolean isRed() {
            return color == Color.RED;
        }

        boolean isBlack() {
            return color == Color.BLACK;
        }

        Node parent() {
            return parent;
        }
        Node sibling() {
            if (parent() == null) return null;
            return (this == parent.left) ? parent.right : parent.left;
        }
        Node nearestNephew() {
            if (sibling() == null) return null;
            return (this == parent.left) ? sibling().left : sibling().right;
        }
        Node furthestNephew() {
            if (sibling() == null) return null;
            return (this == parent.left) ? sibling().right : sibling().left;
        }
        Node grandParent() {
            return (parent() != null) ? parent.parent : null;
        }
        Node uncle() {
            Node gp = grandParent();
            if (gp == null) return null;
            return (parent == gp.left) ? gp.right : gp.left;
        }
    }

    private Node root;
    private Node lastInsertedNode;

    public PieceTree() {
        this.root = null;
        this.lastInsertedNode = null;
    }

    public PieceTree(Piece initial) {if (initial != null) this.root = new Node(initial);}

    public int length() {return (root != null) ? root.length : 0;}

    private void collectText(Node node, StringBuilder stringBuilder, String originalBuffer, StringBuilder addBuffer) {
        if (node == null) {
            return;
        }
        if (node.isLeaf()) {
            String text = node.piece.getText(originalBuffer, addBuffer);
            stringBuilder.append(text);
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

    private void collectPieces(Node node, List<Piece> out) {
        if (node == null) return;
        if (node.isLeaf()) out.add(node.piece);
        else {
            collectPieces(node.left, out);
            collectPieces(node.right, out);
        }
    }

    public List<Piece> toPieceList() {
        List<Piece> out = new ArrayList<>();
        collectPieces(root, out);
        return out;
    }

    private PieceTable.TextLocation findLocationRecursive(Node node, int position) {
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

    public PieceTable.TextLocation findLocation(int position) {
        if (root == null) return null;
        if (position < 0) position = 0;
        if (position > root.length) position = root.length;
        return findLocationRecursive(root, position);
    }

    public void rotateLeft(Node x) {
        if (x == null || x.right == null) return;
        Node y = x.right;
        x.right = y.left;
        if (y.left != null) y.left.parent = x;

        setOldParentToGrandparent(x.parent, x, y);
    }

    public void rotateRight(Node x) {
        if (x == null || x.left == null) return;
        Node y = x.left;
        x.left = y.right;
        if (y.right != null) y.right.parent = x;

        setOldParentToGrandparent(x.parent, x, y);
    }

    private void bubbleRecalc(Node start) {
        Node cur = start;
        while (cur != null) {
            cur.recalc();
            cur = cur.parent;
        }
    }

    private void updateParentChild(Node oldChild, Node newChild) {
        Node parent = (oldChild != null) ? oldChild.parent : null;

        if (newChild == parent) {
            throw new IllegalStateException("Attempted to set parent as its own child");
        }

        if (parent == null) {
            root = newChild; // this would occur when the root node is the only leaf, and it's being completely replaced
        } else if (parent.left == oldChild) {
            parent.left = newChild;
        } else {
            parent.right = newChild;
        }
        if (newChild != null) {
            newChild.parent = parent;
        }

        Node start = (newChild != null) ? newChild : parent; // recalculate based on the leaf, if no leaf update parent weight
        if (start != null) bubbleRecalc(start);
    }

    private void setOldParentToGrandparent(Node futureGrand, Node child, Node newParent) {
        if (newParent == futureGrand) {
            throw new IllegalStateException("Attempted to set parent as its own child");
        }
        if (futureGrand == null) {
            root = newParent; // this would occur when the root node is the only leaf, and it's being completely replaced
        } else if (futureGrand.left == child) {
            futureGrand.left = newParent;
        } else {
            futureGrand.right = newParent;
        }

        if (newParent != null) {
            newParent.parent = futureGrand;
        }
        Node start = (newParent != null) ? newParent : futureGrand; // recalculate based on the leaf, if no leaf update parent weight
        if (start != null) bubbleRecalc(start);
    }

    private void addSiblingNode(Node oldNode, Node newNode, boolean newOnLeft) {
        Node grandparent = oldNode.parent; // this was originally the parent of the node that needs a sibling

        Node newParent = newOnLeft ? new Node(newNode, oldNode) : new Node(oldNode, newNode);
        setOldParentToGrandparent(grandparent, oldNode, newParent);
    }
    private void splitLeafNode(Node oldNode, Node newNode, int offset) {
        Node grandparent = oldNode.parent;

        Piece oldPiece = oldNode.piece;
        int oldLength = oldPiece.getLength();

        Piece leftPiece = new Piece(oldPiece.getSource(), oldPiece.getStart(), offset);
        Piece rightPiece = new Piece(oldPiece.getSource(), oldPiece.getStart() + offset, oldLength - offset);
        Node leftNode = new Node(leftPiece);
        Node rightNode = new Node(rightPiece);

        Node rightSubTree = new Node(newNode, rightNode);
        Node newParent = new Node(leftNode, rightSubTree);
        setOldParentToGrandparent(grandparent, oldNode, newParent);
    }

    private Node insertRecursive(Node node, int position, Piece pieceToInsert) {
        if (node.isLeaf()) {
            Piece old = node.piece;
            int oldLen = old.getLength();
            int offset = Math.max(0, Math.min(position, oldLen));

            if (offset == 0) {
                Node newLeaf = new Node(pieceToInsert);
                addSiblingNode(node, newLeaf, true);
                return newLeaf;

            } else if (offset == oldLen) {
                // new piece after current leaf
                Node newNode = new Node(pieceToInsert);
                addSiblingNode(node, newNode, false);
                return newNode;

            } else {
                Node newNode = new Node(pieceToInsert);
                splitLeafNode(node, newNode, offset);
                return newNode;
            }
        }

        int leftLen = (node.left != null) ? node.left.length : 0;

        if (position < leftLen) {
            return insertRecursive(node.left, position, pieceToInsert);
        } else {
            return insertRecursive(node.right, position - leftLen, pieceToInsert);
        }
    }

    private void insertFixup(Node node) {
        // Continue until we reach the root or parent is black
        while (node != null && node.parent != null && node.parent.isRed()) {
            Node parent = node.parent;
            Node grandparent = parent.parent;

            if (grandparent == null) break;

            if (parent == grandparent.left) {
                // Parent is a left child
                Node uncle = grandparent.right;

                if (uncle != null && uncle.isRed()) {
                    // Case 1: Uncle is red - just recolor
                    parent.color = Color.BLACK;
                    uncle.color = Color.BLACK;
                    grandparent.color = Color.RED;
                    node = grandparent;  // Move up and check again
                } else {
                    // Uncle is black - we need rotations
                    if (node == parent.right) {
                        // Case 2: Node is right child - rotate left first
                        node = parent;
                        rotateLeft(node);
                    }
                    parent = node.parent;
                    if (parent != null) parent.color = Color.BLACK;
                    if (parent != null && parent.parent != null) {
                        parent.parent.color = Color.RED;
                        rotateRight(parent.parent);
                    }
                }
            } else {
                // Parent is a right child - mirror image of above
                Node uncle = grandparent.left;

                if (uncle != null && uncle.isRed()) {
                    parent.color = Color.BLACK;
                    uncle.color = Color.BLACK;
                    grandparent.color = Color.RED;
                    node = grandparent;
                } else {
                    if (node == parent.left) {
                        node = parent;
                        rotateRight(node);

                    }
                    parent = node.parent;
                    if (parent != null) parent.color = Color.BLACK;
                    if (parent != null && parent.parent != null) {
                        parent.parent.color = Color.RED;
                        rotateLeft(parent.parent);
                    }
                }
            }
        }

        if (root != null) root.color = Color.BLACK;
    }

    public void insert(int position, Piece pieceToInsert) {
        if (pieceToInsert == null || pieceToInsert.getLength() == 0) return;

        if (position < 0) position = 0;
        int treeLength = (root != null) ? root.length : 0;
        if (position > treeLength) position = treeLength;

        if (root == null) {
            root = new Node(pieceToInsert);
            root.color = Color.BLACK;
            return;
        }

        Node insertedNode = insertRecursive(root, position, pieceToInsert);

        insertFixup(insertedNode);
    }

    private void removeFixup(Node problemNode) {
        while (problemNode != root && problemNode.isBlack()) {
            if (problemNode == problemNode.parent.left) {
                Node sibling = problemNode.parent.right;

                if (sibling.isRed()) {
                    sibling.color = problemNode.parent.color;
                    problemNode.parent.color = Color.RED;
                    rotateLeft(problemNode.parent);
                    sibling = problemNode.parent.right;
                }
                if ((sibling.left == null || sibling.left.isBlack()) && (sibling.right == null || sibling.right.isBlack())) {
                    sibling.color = Color.RED;
                    problemNode = problemNode.parent;
                } else {
                    if (sibling.right == null || sibling.right.isBlack()) {
                        if (sibling.left != null) {
                            sibling.left.color = Color.BLACK;
                        }
                        sibling.color = Color.RED;
                        rotateRight(sibling);
                        sibling = problemNode.parent.right;
                    }
                    sibling.color = problemNode.parent.color;
                    problemNode.parent.color = Color.BLACK;
                    if (sibling.right != null) sibling.right.color = Color.BLACK;
                    rotateLeft(problemNode.parent);
                    problemNode = root;
                }
            } else {
                Node sibling = problemNode.parent.left;

                if (sibling.isRed()) {
                    sibling.color = problemNode.parent.color;
                    problemNode.parent.color = Color.RED;
                    rotateRight(problemNode.parent);
                    sibling = problemNode.parent.left;
                }

                if ((sibling.left == null || sibling.left.isBlack()) && (sibling.right == null || sibling.right.isBlack())) {
                    sibling.color = Color.RED;
                    problemNode = problemNode.parent;

                } else {
                    if (sibling.left == null || sibling.left.isBlack()) {
                        if (sibling.right != null) sibling.right.color = Color.BLACK;
                        sibling.color = Color.RED;
                        rotateLeft(sibling);
                        sibling = problemNode.parent.left;
                    }
                    sibling.color = problemNode.parent.color;
                    problemNode.parent.color = Color.BLACK;
                    if (sibling.right != null) sibling.left.color = Color.BLACK;
                    rotateRight(problemNode.parent);
                    problemNode = root;
                }
            }
        }
        problemNode.color = Color.BLACK;
    }

    private Node removeRecursive(Node node, int position, int removeLength) {
        if (node == null || removeLength <= 0) return node;

        if (node.isLeaf()) {
            int pieceLen = node.piece.getLength();
            int start = Math.max(0, Math.min(position, pieceLen)); // start point of the deletion
            int end = Math.max(0, Math.min(position + removeLength, pieceLen)); // end point of the deletion
            if (start >= end) return node; // nothing to remove in this leaf

            int leftLen = start; // length of the left portion of the string after deletion
            int rightLen = pieceLen - end; // length of the right portion of the string after deletion

            if (leftLen > 0 && rightLen > 0) {
                Piece leftPiece = new Piece(node.piece.getSource(), node.piece.getStart(), leftLen);
                Piece rightPiece = new Piece(node.piece.getSource(), node.piece.getStart() + end, rightLen);

                Node leftNode = new Node(leftPiece);
                Node rightNode = new Node(rightPiece);
                Node newParent = new Node(leftNode, rightNode);

                newParent.color = node.color;

                updateParentChild(node, newParent);
                return null;

            } else if (leftLen > 0) {
                Piece leftPiece = new Piece(node.piece.getSource(), node.piece.getStart(), leftLen);
                Node leftNode = new Node(leftPiece);

                leftNode.color = node.color;

                updateParentChild(node, leftNode);
                return null;

            } else if (rightLen > 0) {
                Piece rightPiece = new Piece(node.piece.getSource(), node.piece.getStart() + end, rightLen);
                Node rightNode = new Node(rightPiece);

                rightNode.color = node.color;

                updateParentChild(node, rightNode);
                return null;
            } else {
                updateParentChild(node, null);
                return node;
            }
        }

        int leftSubLen = (node.left != null) ? node.left.length : 0;
        Node removedNode = null;

        if (position + removeLength <= leftSubLen) {
            // deletion entirely in left subtree
            removedNode = removeRecursive(node.left, position, removeLength);
        } else if (position >= leftSubLen) {
            // entirely in right subtree
            removedNode = removeRecursive(node.right, position - leftSubLen, removeLength);
        } else {

            int removeFromLeft = leftSubLen - position;
            Node leftRemoved = removeRecursive(node.left, position, removeFromLeft);
            int remaining = removeLength - removeFromLeft;
            Node rightRemoved = removeRecursive(node.right, 0, remaining);

            removedNode = (leftRemoved != null) ? leftRemoved : rightRemoved; // in this case we'll delete the left if there was a spanning deletion
        }

        node.recalc(); // recalculate the node's length since children may have changed

        if (node.left == null && node.right == null) {
            updateParentChild(node, null);
            return node;
        } else if (node.left == null) {
            node.right.color = node.color;
            updateParentChild(node, node.right);
            return node;
        } else if (node.right == null) {
            node.left.color = node.color;
            updateParentChild(node, node.left);
            return node;
        }
        return removedNode;
    }

    private Node combine(Node left, Node right) {
        if (left == null) return right;
        if (right == null) return left;
        return new Node(left, right);
    }

    public void remove(int position, int removeLength) {
        if (removeLength <= 0 || root == null) return;

        int treeLength = root.length;
        if (position < 0) position = 0;
        if (position >= treeLength) return; // nothing to remove
        if (position + removeLength > treeLength) {
            removeLength = treeLength - position; // trim to valid range
        }

        Node removedNode = removeRecursive(root, position, removeLength);
        if (removedNode != null && removedNode.isBlack()) {
            Node replacementNode = findReplacementForRemovedNode(removedNode);
            if (replacementNode != null) {
                removeFixup(replacementNode);
            }
        }

    }

    private Node findReplacementForRemovedNode(Node removedNode) {
        // The removed node's parent should now point to whatever replaced it
        Node parent = removedNode.parent;
        if (parent == null) {
            // Root was removed, new root (if any) is the replacement
            return root;
        }

        // Find what's now in the removed node's position
        if (parent.left == null && parent.right == null) {
            // Parent became a leaf, so nothing replaced the removed node
            // The parent itself needs to be treated as having a "null child" problem
            return parent;
        } else if (parent.left == null) {
            // Left child was removed, right child might be the replacement
            return parent.right;
        } else if (parent.right == null) {
            // Right child was removed, left child might be the replacement
            return parent.left;
        }

        // Both children still exist, so an internal restructuring happened
        // In this case, no single node replacement occurred
        return null;
    }

    public boolean isValidRedBlack() {
        if (root != null && root.isRed()) return false;  // Root must be black
        return checkRedBlackProperties(root) != -1;
    }

    private int checkRedBlackProperties(Node node) {
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

        if (leftHeight == -1 || rightHeight == -1 || leftHeight != rightHeight) {
            return -1;  // Black height violation
        }

        return leftHeight + (node.isBlack() ? 1 : 0);
    }

}
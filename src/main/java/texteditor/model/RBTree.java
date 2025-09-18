package texteditor.model;

public abstract class RBTree<T> {
    private enum Color {RED, BLACK}

    protected static class Node<T> {
        T payload;
        Node<T> left, right, parent;
        Color color = Color.RED;
        int length;

        public Node(T payload) {
            this.payload = payload;
        }

        public Node(Node<T> left, Node<T> right) {
            this.left = left;
            this.right = right;
            if (left != null) left.parent = this;
            if (right != null) right.parent = this;

        }

        boolean isRed() {
            return color == Color.RED;
        }

        boolean isBlack() {
            return color == Color.BLACK;
        }
    }

    protected Node<T> root;

    protected abstract void recompute(Node<T> node);

    protected abstract int payloadLength(T payload);

    protected void bubbleRecompute(Node<T> start) {
        Node<T> curr = start;
        while (curr != null) {
            recompute(curr);
            curr = curr.parent;
        }
    }

    protected int length() {
        return (root != null) ? root.length : 0;
    }

    protected void replaceChild(Node<T> parent, Node<T> oldChild, Node<T> newChild) {
        if (parent == newChild) {
            throw new IllegalStateException("Attempted to set parent as its own child");
        }
        if (parent == null) {
            root = newChild;
            if (newChild != null) newChild.parent = null;
        } else if (parent.left == oldChild) {
            parent.left = newChild;
        } else {
            parent.right = newChild;
        }

        if (newChild != null) newChild.parent = parent;
        bubbleRecompute(newChild != null ? newChild : parent);
    }

    protected void rotateLeft(Node<T> x) {
        if (x == null || x.right == null) return;
        Node<T> y = x.right;

        // 1) move y.left to x.right
        x.right = y.left;
        if (y.left != null) y.left.parent = x;

        // 2) attach y to x.parent
        Node<T> xParent = x.parent;
        y.parent = xParent;
        if (xParent == null) root = y;
        else if (xParent.left == x) xParent.left = y;
        else xParent.right = y;

        // 3) make x left child of y
        y.left = x;
        x.parent = y;

        // 4) recalc lengths bottom-up
        recompute(x);
        recompute(y);
        bubbleRecompute(y.parent);
    }

    protected void rotateRight(Node<T> x) {
        if (x == null || x.left == null) return;
        Node<T> y = x.left;
        x.left = y.right;
        if (y.right != null) y.right.parent = x;

        Node<T> xParent = x.parent;
        y.parent = xParent;
        if (xParent == null) root = y;
        else if (xParent.left == x) xParent.left = y;
        else xParent.right = y;

        y.right = x;
        x.parent = y;

        recompute(x);
        recompute(y);
        bubbleRecompute(y.parent);
    }

    protected abstract Node<T> insertRecursive(Node<T> node, int position, T payload);

    protected void insertFixup(Node<T> node) {
        while (node != null && node.parent != null && node.parent.isRed()) {
            Node<T> parent = node.parent;
            Node<T> grandparent = parent.parent;

            if (grandparent == null) break;

            if (parent == grandparent.left) {
                // Parent is a left child
                Node<T> uncle = grandparent.right;

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
                Node<T> uncle = grandparent.left;

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

    protected void insert(int position, T payload) {
        if (payload == null || payloadLength(payload) == 0) return;

        if (position < 0) position = 0;
        int treeLength = (root != null) ? root.length : 0;
        if (position > treeLength) position = treeLength;

        if (root == null) {
            root = new Node(payload);
            root.color = Color.BLACK;
            return;
        }

        Node insertedNode = insertRecursive(root, position, payload);

        insertFixup(insertedNode);
    }

    protected abstract Node<T> removeRecursive(Node root, int position, int removeLength);

    private Node<T> findNodeForFixup(Node<T> removedNode) {
        // The removed node's parent should now point to whatever replaced it
        Node<T> parent = removedNode.parent;
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

    protected void removeFixup(Node<T> problemNode) {
        if (problemNode == null) {
            throw new IllegalArgumentException("Node not found");
        }

        while (problemNode != root && problemNode.isBlack()) {
            if (problemNode == problemNode.parent.left) {
                Node<T> sibling = problemNode.parent.right;

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
                Node<T> sibling = problemNode.parent.left;

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
                    if (sibling.left != null) sibling.left.color = Color.BLACK;
                    rotateRight(problemNode.parent);
                    problemNode = root;
                }
            }
        }
        problemNode.color = Color.BLACK;
    }

    protected void remove(int position, int removeLength) {
        if (removeLength <= 0 || root == null) return;

        int treeLength = root.length;
        if (position < 0) position = 0;
        if (position >= treeLength) return; // nothing to remove
        if (position + removeLength > treeLength) {
            removeLength = treeLength - position; // trim to valid range
        }

        Node<T> removedLeafNode = removeRecursive(root, position, removeLength);
        if (removedLeafNode != null && removedLeafNode.isBlack()) {
            Node<T> problemNode = findNodeForFixup(removedLeafNode);
            removeFixup(problemNode);
        }
    }
}

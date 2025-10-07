package texteditor.model;

public abstract class RBTree<N extends RBTree.Node<N,P>, P> {
    enum Color {RED, BLACK}

    protected abstract static class Node<N extends RBTree.Node<N,P>, P> {
        P payload;
        N left, right, parent;
        int length;
        Color color;

        protected Node(P payload) {
            this.payload = payload;
            this.left = this.right = this.parent = null;
            this.color = Color.RED;
        }

        protected Node(N left, N right) {
            this.payload = null;
            this.left = left;
            this.right = right;
            this.parent = null;
            this.color = Color.BLACK;
        }

        protected boolean isRed() {
            return color == Color.RED;
        }

        protected boolean isBlack() {
            return color == Color.BLACK;
        }

        protected abstract boolean isLeaf();

        /*@Override
        public String toString() {
            String role =  (isLeaf()) ? "L" : "I";
            return String.format(
                    "[%s %s len=%d]",
                    role,
                    isRed() ? "Red" : "Black",
                    length
            );
        }

         */
    }

    protected N root;

    protected abstract void setRoot(N root);
    protected abstract N getRoot();

    protected abstract N createLeafNode(P payload);
    protected abstract N createInternalNode(N left, N right);

    protected abstract void addSiblingNode(N oldNode, N newNode, boolean newOnLeft);
    protected abstract void recompute(N node);

    protected void bubbleRecompute(N start) {
        N curr = start;
        while (curr != null) {
            recompute(curr);
            curr = curr.parent;
        }
    }

    protected abstract int treeLength();

    protected void replaceChild(N parent, N oldChild, N newChild) {
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

    protected void rotateLeft(N x) {
        if (x == null || x.right == null) return;
        N y = x.right;

        // 1) move y.left to x.right
        x.right = y.left;
        if (y.left != null) y.left.parent = x;

        // 2) attach y to x.parent
        N xParent = x.parent;
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

    protected void rotateRight(N x) {
        if (x == null || x.left == null) return;
        N y = x.left;
        x.left = y.right;
        if (y.right != null) y.right.parent = x;

        N xParent = x.parent;
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

    protected void insertFixup(N node) {
        while (node != null && node.parent != null && node.parent.isRed()) {
            N parent = node.parent;
            N grandparent = parent.parent;

            if (grandparent == null) break;

            if (parent == grandparent.left) {
                // Parent is a left child
                N uncle = grandparent.right;

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
                N uncle = grandparent.left;

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

    protected N findNodeForFixup(N removedNode) {
        // The removed node's parent should now point to whatever replaced it
        N parent = removedNode.parent;
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

    protected void removeFixup(N problemNode) {
        if (problemNode == null) {
            throw new IllegalArgumentException("Node not found");
        }

        while (problemNode != root && problemNode.isBlack()) {
            if (problemNode == problemNode.parent.left) {
                N sibling = problemNode.parent.right;

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
                N sibling = problemNode.parent.left;

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
}

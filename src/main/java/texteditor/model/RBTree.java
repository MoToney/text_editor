package texteditor.model;

public abstract class RBTree<N extends RBTree.Node<N,P>, P> {
    protected enum Color {RED, BLACK}

    protected abstract static class Node<N extends RBTree.Node<N,P>, P> {
        P payload;
        N left, right, parent;
        int lftSize;
        Color color;

        protected Node(P payload, N left, N right, Color color) {
            this.payload = payload;
            this.left = left;
            this.right = right;
            this.parent = null;
            this.color = color;
            this.lftSize = 0;

        }

        protected Node(P payload) {
            this(payload, null, null, Color.RED);
        }

        protected Node(P payload, N left, N right) {
            this(payload, left, right, Color.RED);
        }

        protected boolean isRed() {
            return color == Color.RED;
        }

        protected boolean isBlack() {
            return color == Color.BLACK;
        }
    }

    protected N root;
    protected final N NIL;
    protected abstract N createSentinel();
    protected abstract boolean isLeaf(N node);

    RBTree() {
        this.NIL = createSentinel();
        setRoot(this.NIL);
    }

    //TODO: add SENTINEL/this.NIL node functionality

    protected void setRoot(N node) {
        this.root = node;
        this.root.color = Color.BLACK;
        this.root.parent = this.NIL;
        recompute(this.root);
    }

    protected void detach(N node) {
        node.left = node.right = node.parent = this.NIL;
    }

    protected N getRoot() {
        return this.root;
    }

    protected abstract N createNode(P payload);

    protected abstract void recompute(N node);

    protected void bubbleRecompute(N start) {
        N curr = start;
        while (curr != this.NIL) {
            recompute(curr);
            curr = curr.parent;
        }
    }

    protected abstract int length();

    protected void replaceChild(N parent, N oldChild, N newChild) {
        if (parent == newChild) { throw new IllegalStateException("Attempted to set parent as its own child"); }

        if (parent == this.NIL) { root = newChild;
        } else if (parent.left == oldChild) { parent.left = newChild;
        } else { parent.right = newChild; }

        if (newChild != this.NIL) newChild.parent = parent;
        bubbleRecompute(newChild != this.NIL ? newChild : parent);
    }

    protected void rotateLeft(N x) {
        if (x == this.NIL || x.right == this.NIL) return;
        N y = x.right;

        // 1) move y.left to x.right
        x.right = y.left;
        if (y.left != this.NIL) y.left.parent = x;

        // 2) attach y to x.parent
        N xParent = x.parent;
        y.parent = xParent;
        if (xParent == this.NIL) root = y;
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
        if (x == this.NIL || x.left == this.NIL) return;
        N y = x.left;
        x.left = y.right;
        if (y.right != this.NIL) y.right.parent = x;

        N xParent = x.parent;
        y.parent = xParent;
        if (xParent == this.NIL) root = y;
        else if (xParent.left == x) xParent.left = y;
        else xParent.right = y;

        y.right = x;
        x.parent = y;

        recompute(x);
        recompute(y);
        bubbleRecompute(y.parent);
    }

    protected void insertFixup(N node) {
        while (node != this.NIL && node.parent != this.NIL && node.parent.isRed()) {
            N parent = node.parent;
            N grandparent = parent.parent;

            if (grandparent == this.NIL) break;

            if (parent == grandparent.left) {
                // Parent is a left child
                N uncle = grandparent.right;

                if (uncle != this.NIL && uncle.isRed()) {
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
                    if (parent != this.NIL) parent.color = Color.BLACK;
                    if (parent != this.NIL && parent.parent != this.NIL) {
                        parent.parent.color = Color.RED;
                        rotateRight(parent.parent);
                    }
                }
            } else {
                // Parent is a right child - mirror image of above
                N uncle = grandparent.left;

                if (uncle != this.NIL && uncle.isRed()) {
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
                    if (parent != this.NIL) parent.color = Color.BLACK;
                    if (parent != this.NIL && parent.parent != this.NIL) {
                        parent.parent.color = Color.RED;
                        rotateLeft(parent.parent);
                    }
                }
            }
        }
        if (root != this.NIL) root.color = Color.BLACK;
    }

    protected N findNodeForFixup(N removedNode) {
        // The removed node's parent should now point to whatever replaced it
        N parent = removedNode.parent;
        if (parent == this.NIL) {
            // Root was removed, new root (if any) is the replacement
            return root;
        }
        // Find what's now in the removed node's position
        if (parent.left == this.NIL && parent.right == this.NIL) {
            // Parent became a leaf, so nothing replaced the removed node
            // The parent itself needs to be treated as having a "this.NIL child" problem
            return parent;
        } else if (parent.left == this.NIL) {
            // Left child was removed, right child might be the replacement
            return parent.right;
        } else if (parent.right == this.NIL) {
            // Right child was removed, left child might be the replacement
            return parent.left;
        }
        // Both children still exist, so an internal restructuring happened
        // In this case, no single node replacement occurred
        return this.NIL;
    }

    protected abstract void deleteNode(N node);

    protected void removeFixup(N problemNode) {

        while (problemNode != root && problemNode != NIL && problemNode.isBlack()) {
            if (problemNode == problemNode.parent.left) {
                N sibling = problemNode.parent.right;

                if (sibling.isRed()) {
                    sibling.color = problemNode.parent.color;
                    problemNode.parent.color = Color.RED;
                    rotateLeft(problemNode.parent);
                    sibling = problemNode.parent.right;
                }
                if ((sibling.left == this.NIL || sibling.left.isBlack()) && (sibling.right == this.NIL || sibling.right.isBlack())) {
                    sibling.color = Color.RED;
                    problemNode = problemNode.parent;
                } else {
                    if (sibling.right == this.NIL || sibling.right.isBlack()) {
                        if (sibling.left != this.NIL) {
                            sibling.left.color = Color.BLACK;
                        }
                        sibling.color = Color.RED;
                        rotateRight(sibling);
                        sibling = problemNode.parent.right;
                    }
                    sibling.color = problemNode.parent.color;
                    problemNode.parent.color = Color.BLACK;
                    if (sibling.right != this.NIL) sibling.right.color = Color.BLACK;
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

                if ((sibling.left == this.NIL || sibling.left.isBlack()) && (sibling.right == this.NIL || sibling.right.isBlack())) {
                    sibling.color = Color.RED;
                    problemNode = problemNode.parent;

                } else {
                    if (sibling.left == this.NIL || sibling.left.isBlack()) {
                        if (sibling.right != this.NIL) sibling.right.color = Color.BLACK;
                        sibling.color = Color.RED;
                        rotateLeft(sibling);
                        sibling = problemNode.parent.left;
                    }
                    sibling.color = problemNode.parent.color;
                    problemNode.parent.color = Color.BLACK;
                    if (sibling.left != this.NIL) sibling.left.color = Color.BLACK;
                    rotateRight(problemNode.parent);
                    problemNode = root;
                }
            }
        }
        problemNode.color = Color.BLACK;
    }

    protected void addToRight(N node, P payload) {
        N newNode = createNode(payload);

        if (root == this.NIL) {
            setRoot(newNode);
        }

        else if (node.right == this.NIL) {
            node.right = newNode;
            newNode.parent = node;
        }
        else {
            N properNode = leftmost(node.right);
            properNode.left = newNode;
            newNode.parent = properNode;
        }

        insertFixup(newNode);
        bubbleRecompute(newNode);
    }

    protected void addToLeft(P payload, N node) {
        N newNode = createNode(payload);
        if (root == this.NIL) {
            setRoot(newNode);
        }
        else if (node.left == this.NIL) {
            node.left = newNode;
            newNode.parent = node;
        }
        else {
            N properNode = rightmost(node.left);
            properNode.right = newNode;
            newNode.parent = properNode;
        }
        bubbleRecompute(newNode);
        insertFixup(newNode);
    }

    protected N leftmost(N node) {
        N currentNode = node;
        while (currentNode != NIL) {
            if (currentNode.left != NIL) {
                currentNode = currentNode.left;
            } else {
                break;
            }
        }
        return currentNode;
    }

    protected N firstLeaf() {
        return leftmost(root);
    }

    protected N rightmost(N node) {
        N currentNode = node;
        while (currentNode != NIL ) {
            if (currentNode.right != NIL) {
                currentNode = currentNode.right;
            } else {
                break;
            }
        }
        return currentNode;
    }

    protected N lastLeaf() {
        return rightmost(root);
    }

    protected N nextNode(N node) {
        if (node == this.NIL) return this.NIL;

        if (node.right != this.NIL) {
            return leftmost(node.right);
        }

        N child = node;
        while (child.parent != this.NIL && child.parent.right == child) {
            child = child.parent;
        }
        return child.parent;
    }

    protected N prevNode(N node) {
        if (node == this.NIL) return this.NIL;

        if (node.left != this.NIL) {
            return rightmost(node.left);
        }

        N child = node;
        while (child.parent != this.NIL && child.parent.left == child) {
            child = child.parent;
        }
        return child.parent;
    }
}

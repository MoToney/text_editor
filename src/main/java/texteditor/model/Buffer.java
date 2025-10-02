package texteditor.model;

abstract class Buffer {
    private final Type type;

    public enum Type { ORIGINAL, ADD }

    protected Buffer(Type type) {
        this.type = type;
    }

    public Type getType() { return type; }

    public abstract String toString();

    public abstract int length();
    public abstract String substring(int start, int end);
    public abstract char charAt(int index);

}

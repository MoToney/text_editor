package texteditor.model;

public class AddBuffer extends Buffer {
    private final StringBuilder storage;

    public AddBuffer() {
        super(Type.ADD);
        storage = new StringBuilder();
    }

    @Override
    public String toString() { return storage.toString(); }

    @Override
    public int length() { return storage.length(); }

    @Override
    public String substring(int start, int end) { return storage.substring(start, end); }

    @Override
    public char charAt(int index) { return storage.charAt(index); }

    public void append(String text) { storage.append(text); }
}

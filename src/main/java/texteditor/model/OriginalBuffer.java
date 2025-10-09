package texteditor.model;

public class OriginalBuffer extends Buffer {
    private final String storage;

    public OriginalBuffer(String storage) {
        super(Type.ORIGINAL);
        this.storage = storage;
    }

    @Override
    public String toString() {
        int previewLength = Math.min(100, storage.length());
        String preview = storage.substring(0, previewLength);
        return "OriginalBuffer[len=" + storage.length() + ", preview=\"" + preview + (storage.length() > 100 ? "...\"" : "\"") + "]";
    }

    @Override
    public String getText() { return storage; }

    @Override
    public int length() { return storage.length(); }

    @Override
    public String substring(int start, int end) { return storage.substring(start, end); }

    @Override
    public char charAt(int index) { return storage.charAt(index); }

}

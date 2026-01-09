package followisp;

public class ReadOnlyDocument implements Document{
    private final String data;

    public ReadOnlyDocument(String data) {
        this.data = data;
    }

    @Override
    public void open() {
        System.out.println("Read-Only Document opened. Data: " + preview());
    }

    @Override
    public String getData() {
        return data;
    }

    private String preview() {
        return data.substring(0, Math.min(data.length(), 20)) + "...";
    }
}

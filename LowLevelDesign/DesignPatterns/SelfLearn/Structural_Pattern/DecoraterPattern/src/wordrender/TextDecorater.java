package wordrender;

public abstract class TextDecorater implements TextView{

    protected final TextView inner;

    public TextDecorater(TextView inner)
    {
        this.inner = inner;
    }

    @Override
    public void render() {
        inner.render(); // Delegate to the wrapped object
    }
}

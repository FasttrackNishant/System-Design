package decorater;

public abstract class TextDecorater implements TextView{

    protected final TextView inner;

    public TextDecorater(TextView inner){
        this.inner = inner;
    }
}
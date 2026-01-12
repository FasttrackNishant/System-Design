package structural.decorater;


class ItalicDecorater extends TextDecorater{

    public ItalicDecorater(TextView inner){
        super(inner);
    }

    @Override
    public void render() {
        System.out.print("<i>");
        inner.render();
        System.out.print("</i>");
    }
}
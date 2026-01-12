package structural.decorater;

class BoldDecorater extends TextDecorater{

    public BoldDecorater(TextView inner){
        super(inner);
    }

    @Override
    public void render() {
        System.out.print("<b>");
        inner.render();
        System.out.print("</b>");
    }

}
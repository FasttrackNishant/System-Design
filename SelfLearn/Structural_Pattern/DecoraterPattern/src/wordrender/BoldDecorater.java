package wordrender;

public class BoldDecorater extends  TextDecorater{

    public BoldDecorater(TextView inner)
    {
        super(inner);
    }

    @Override
    public void render()
    {
        System.out.print("<b>");
        super.render();
        System.out.print("</b>");
    }
}

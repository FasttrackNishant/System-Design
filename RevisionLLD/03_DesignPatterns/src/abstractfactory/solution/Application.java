package abstractfactory.solution;

public class Application {

    private final Button button;
    private final Checkbox checkbox;

    public Application(GUIFactory factory){
        button = factory.createButton();
        checkbox = factory.createCheckbox();
    }

    public void renderUI()
    {
        button.paint();
        checkbox.paint();
    }

}

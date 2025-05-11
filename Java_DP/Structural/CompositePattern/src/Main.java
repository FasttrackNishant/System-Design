public class Main {
    public static void main(String[] args) {

        UiComponent uiComponent = new Menu();

        uiComponent.add(new Button());
        uiComponent.add(new Button());
        Button btn = new Button();
        uiComponent.add(btn);
        uiComponent.draw();

        uiComponent.remove(btn);

        System.out.println("final add ");

        uiComponent.draw();
    }
}
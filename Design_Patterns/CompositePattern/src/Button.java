public class Button implements UiComponent {

    @Override
    public void draw() {
        System.out.println("Drawing");
    }

    @Override
    public void add(UiComponent component) {
        System.out.println("Adding");
    }

    @Override
    public void remove(UiComponent component) {
        System.out.println("Removing");
    }
}

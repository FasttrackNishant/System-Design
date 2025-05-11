import java.util.ArrayList;
import java.util.List;

public class Menu implements UiComponent {

    // ismein bohot sare ho skate hain

    // iske andar ke sare parts ko children bolenge

    List<UiComponent> children = new ArrayList<>();


    @Override
    public void draw() {
        for (UiComponent cmp : children) {
            cmp.draw();
        }
    }

    @Override
    public void add(UiComponent component) {

        children.add(component);
    }

    @Override
    public void remove(UiComponent component) {
        children.remove(component);
    }
}

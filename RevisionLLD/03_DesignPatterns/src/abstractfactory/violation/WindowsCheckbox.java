package abstractfactory.violation;

public class WindowsCheckbox {
    public void paint() {
        System.out.println("Painting a Windows-style checkbox.");
    }

    public void onSelect() {
        System.out.println("Windows checkbox selected.");
    }
}
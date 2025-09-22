package OperatingSystem.Optimal;

public class WindowsCheckbox implements Checkbox{

    @Override
    public void paint() {
        System.out.println("Windows Checkbox painted");
    }

    @Override
    public void onSelect() {
        System.out.println("Widows Checkbox Selected");

    }
}

package OperatingSystem.Optimal;

public class MacOSCheckbox implements Checkbox{
    @Override
    public void paint() {
        System.out.println("Mac OS Checkbox painted");
    }

    @Override
    public void onSelect() {
        System.out.println("Mac os check box selected");

    }
}

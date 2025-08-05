package OperatingSystem.Optimal;

public class WindowsButton implements  Button{
    @Override
    public void paint() {
        System.out.println("Windows button is painting");
    }

    @Override
    public void onClick() {
        System.out.println("Windows button is clicke");
    }
}

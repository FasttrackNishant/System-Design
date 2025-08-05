package OperatingSystem.Optimal;

public class MacOSButton implements  Button{
    @Override
    public void paint() {
        System.out.println("Mac Os button painting");
    }

    @Override
    public void onClick() {
        System.out.println("Mac os button Clicked");
    }
}

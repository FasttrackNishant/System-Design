import OperatingSystem.Naive.MacOSNaive.MacOSButton;
import OperatingSystem.Naive.WindowsNaive.WindowsButton;
import OperatingSystem.Optimal.Application;
import OperatingSystem.Optimal.GUIFactory;
import OperatingSystem.Optimal.MacOSFactory;
import OperatingSystem.Optimal.WindowsFactory;

public class Main {
    public static void main(String[] args) {

        String os = "MacOS";

        if(os.contains("Windows"))
        {
            WindowsButton windowsButton = new WindowsButton();
            windowsButton.paint();
            windowsButton.onClick();
        }
        else if(os.contains("MacOS"))
        {
            MacOSButton macOSButton = new MacOSButton();
            macOSButton.paint();
            macOSButton.onClick();
        }


        System.out.println("---------------------Abstract Factory------------------------");
        GUIFactory factory ;
//        factory = new MacOSFactory();
        factory = new WindowsFactory();

        Application app = new Application(factory);
        app.renderUI();
    }
}
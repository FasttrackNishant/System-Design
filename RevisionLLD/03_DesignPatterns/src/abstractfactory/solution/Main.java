package abstractfactory.solution;

public class Main {

    public static void main(String[] args) {

        GUIFactory windows = new WindowsFactory();

        GUIFactory macos = new MacOSFactory();

        Application application = new Application(macos);
        application.renderUI();


    }
}

public class Main {
    public static void main(String[] args) {
        System.out.println("Hello, World!");

        // sabse pehle chain create karlo
        AbstractHandler pressureHandler = new PressueIssueHandler(null);

        EngineIssueHandler engineIssueHandler = new EngineIssueHandler(pressureHandler);

        PressureIssueRequest pressureIssueRequest = new PressureIssueRequest();

        engineIssueHandler.HandleRequest(pressureIssueRequest);
    }
}
public class EngineIssueHandler extends AbstractHandler {

    // same like pressure handler
    public static int code = 301;

    public EngineIssueHandler(AbstractHandler handler) {
        super(handler);
    }

    @Override
    public void HandleRequest(AbstractRequest request) {
        if (request.getRequestCode() == code) {
            System.out.println("Handling request code Matched in Engine Handler");
        } else {
            super.HandleRequest(request);
        }
    }
}

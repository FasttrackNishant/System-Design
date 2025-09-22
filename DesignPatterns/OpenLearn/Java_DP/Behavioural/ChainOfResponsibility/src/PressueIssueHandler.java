public class PressueIssueHandler extends AbstractHandler {

    public static int code = 303;

    public PressueIssueHandler(AbstractHandler handler) {
        super(handler);
    }

    @Override
    public void HandleRequest(AbstractRequest request) {
        if (request.getRequestCode() == code) {
            System.out.println("Handling request code Matched in Pressure Handler");
        } else {
            super.HandleRequest(request);
        }
    }
}

public class AbstractHandler {

    // toh yaha successor ka ref hoga jo aage pass karega

    private AbstractHandler nextHandler;

    public AbstractHandler(AbstractHandler next) {
        this.nextHandler = next;
    }

    public void HandleRequest(AbstractRequest request) {
        if (nextHandler != null) {
            nextHandler.HandleRequest(request);
        }
    }

}

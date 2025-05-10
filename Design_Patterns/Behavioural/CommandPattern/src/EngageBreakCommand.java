public class EngageBreakCommand implements Command {

    // this is receiver
    BreakMechanism breakMechanism;

    public EngageBreakCommand(BreakMechanism breakii) {
        this.breakMechanism = breakii;
    }

    @Override
    public void executor() {
        breakMechanism.applyBreak();

    }
}

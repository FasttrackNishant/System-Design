public class AirSuspensionCommand implements Command {

    AirSuspensionMechanism airSuspensionMechanism;

    @Override
    public void executor() {
        airSuspensionMechanism.liftSuspension();
    }

    public AirSuspensionCommand(AirSuspensionMechanism airSuspension) {
        this.airSuspensionMechanism   = airSuspension;
    }


}

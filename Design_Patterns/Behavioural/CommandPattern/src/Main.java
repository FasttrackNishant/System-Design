public class Main {
    public static void main(String[] args) {
        System.out.println("Hello, World!");

        //cmd pattern

        BreakMechanism breakMechanism = new BreakMechanism();
        AirSuspensionMechanism airSuspensionMechanism = new AirSuspensionMechanism();

        EngageBreakCommand breakCommand = new EngageBreakCommand(breakMechanism);
        AirSuspensionCommand airCmd = new AirSuspensionCommand(airSuspensionMechanism);

        Panel btnPanel = new Panel();

        btnPanel.setCommand(0, airCmd);

        btnPanel.setCommand(1, breakCommand);


        btnPanel.liftSuspension();
        btnPanel.applyBreak();
    }


}
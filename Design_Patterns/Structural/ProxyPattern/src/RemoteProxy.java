public class RemoteProxy  implements  ICar{
    //yaha scoprio ka object chahiye
    RCScorpio scorpio = new RCScorpio();

    @Override
    public void turnLeft() {
        System.out.println("Turn left in remote class");
        scorpio.turnLeft();
    }

    @Override
    public void turnRight() {
        System.out.println("Turn Right in Remote Class");
    }

    @Override
    public void goStraight() {
        System.out.println("Go Straight in Remote Class");
    }
}

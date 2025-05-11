public class Main {
    public static void main(String[] args) {
        System.out.println("Hello, World!");
        HotAirBalloon hotAirBalloon = new HotAirBalloon();
        Adapter adapter = new Adapter(hotAirBalloon);
        adapter.start();
    }
}
public class Main {
    public static void main(String[] args) {
        System.out.println("Hello, World!");

        AbstractScorpio scorpio = new ScorpioN( new ScorpioImpl_USA());
        System.out.println(scorpio.isRightHanded());
    }
}
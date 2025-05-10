public class Main {
    public static void main(String[] args) {
        System.out.println("Hello, World!");

        Tejas plane = new Tejas();

        for (int i = 0; i < 5; i++) {
            //fetch src location from array
            int srcX = 5;
            int srcY = 10;

            int destX = 100;
            int destY = 515;
            int speed = 15;

            System.out.println("Time " + plane.getTotalTimeToReachDestination(srcX, srcY, destX, destY, speed));


        }
    }
}
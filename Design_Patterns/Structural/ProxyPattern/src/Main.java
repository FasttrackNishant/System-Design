import java.util.Scanner;

public class Main {
    public static void main(String[] args) {
        System.out.println("Hello, World!");

        // Create a Remote Proxy
        RemoteProxy remoteProxy = new RemoteProxy();

        while(true)
        {
        Scanner sc = new Scanner(System.in);
        String action = sc.nextLine();

        switch (action){
            case "left":
                remoteProxy.turnLeft();
                break;

            case "right":
                remoteProxy.turnRight();
                break;

            case "straight":
                remoteProxy.goStraight();
                break;

            default:
                System.out.println("Invlid");
        }

        }

    }
}
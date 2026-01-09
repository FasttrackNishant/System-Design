package singleton;

public class Main {

    public static void main(String[] args) {
        LazySingleton instance = LazySingleton.getInstance();
        System.out.println(instance);

        LazySingleton instance1 = LazySingleton.getInstance();
        System.out.println(instance1);

    }


}

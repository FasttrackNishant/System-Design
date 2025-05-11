public class Main {
    public static void main(String[] args) {
        System.out.println("Hello, World!");

        ICar scorpio = new Scorpio();

        ICar bulletProof = new BulletProof(scorpio);

        float totalWeight = bulletProof.getWeight();
        System.out.println("Final weight is "+ totalWeight );

    }
}
package vehicle;

public class BulletProofDecorator extends CarDecorator{


    public  BulletProofDecorator(Car inner)
    {
        super(inner);
        System.out.println("Bulletproof feature added");
    }


    @Override
    public void start() {
        super.start();
        System.out.println("Bulletproof system active during start");
    }

    @Override
    public void stop() {
        super.stop();
        System.out.println("Bulletproof system remains secure after stop");
    }

    @Override
    public int getWeight() {
        return super.getWeight() + 200;  // bulletproof glass & armor weight
    }
}

package vehicle;

public class Scorpio implements Car{

    public Scorpio()
    {
        System.out.println("Scorpio created");
    }

    @Override
    public void start() {
        System.out.println("Scorpio started");
    }

    @Override
    public void stop() {
        System.out.println("Scorpio Stopped");
    }

    @Override
    public int getWeight() {
        return weight;
    }
}

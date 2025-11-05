package sourcevehicle.models;

import sourcevehicle.ScorpioPrototype;

public class Scorpio implements ScorpioPrototype {

    // default engine
    ScorpioEngine engine = new ScorpioEngine();

    @Override
    public void start()
    {
        System.out.println("Scorpio started");
    }

    @Override
    public ScorpioPrototype clone()
    {
        return new Scorpio();
    }

    @Override
    public void setEngine(ScorpioEngine engine) {
        this.engine = engine;
    }
}

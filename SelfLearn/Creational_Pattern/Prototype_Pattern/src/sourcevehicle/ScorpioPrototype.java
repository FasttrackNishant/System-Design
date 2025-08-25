package sourcevehicle;

import sourcevehicle.models.ScorpioEngine;

public interface ScorpioPrototype {

    ScorpioPrototype clone();

    void setEngine(ScorpioEngine scorpioEngine);

    void start();

}

import sourcevehicle.models.Scorpio;
import sourcevehicle.ScorpioPrototype;
import sourcevehicle.models.ScorpioClassicEngine;
import sourcevehicle.models.ScorpioEngine;
import sourcevehicle.models.ScorpioNEngine;

public class Main {
    public static void main(String[] args) {
        System.out.println("Hello, World!");

        // create a prototype

        ScorpioPrototype prototype = new Scorpio();

        //create a scorpio n
        ScorpioPrototype scorpioN = prototype.clone();
        scorpioN.setEngine(new ScorpioNEngine());

        // create scorpio classic
        ScorpioPrototype scorpioClassic = prototype.clone();
        scorpioClassic.setEngine(new ScorpioClassicEngine());

    }
}
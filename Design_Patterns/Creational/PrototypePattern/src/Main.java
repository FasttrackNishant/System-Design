public class Main {
    public static void main(String[] args)
    {
        System.out.println("Hello, World!");

        //create a prototype
        IScorpioPrototype prototype = new Scorpio();

        //Create a Scorpio N
        IScorpioPrototype scorpioN = prototype.clone();
        scorpioN.setEngine(new ScorpioNEngine());

        //Create a Scorpio Classic
        IScorpioPrototype scorpioClassic = prototype.clone();
        scorpioClassic.setEngine(new ScorpioClassicEngine() );

    }
}
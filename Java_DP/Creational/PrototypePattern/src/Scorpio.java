//public class Scorpio implements IScorpioPrototype{
//
//    //default engine
//    ScorpioEngine engine = new ScorpioEngine();
//
//    @Override
//    public void Start()
//    {
//        System.out.println("Start ho gayi");
//    }
//
//    @Override
//    public IScorpioPrototype clone()
//    {
//        return  new Scorpio();
//    }
//
//    @Override
//    public void setEngine(ScorpioEngine scorpioEngine) {
//        this.engine = scorpioEngine;
//    }
//}


// ***** For Cloneable  - > phehle hi bata dete hain ki clone hoga iske andar
//
//public class Scorpio implements IScorpioPrototype , Cloneable{
//
//    //default engine
//    ScorpioEngine engine = new ScorpioEngine();
//
//    @Override
//    public void Start()
//    {
//        System.out.println("Start ho gayi");
//    }
//
//    @Override
//    public IScorpioPrototype clone () {
//        try{
//        return  (Scorpio) super.clone();
//        }catch (CloneNotSupportedException E)
//        {
//            throw  new  AssertionError();
//        }
//    }
//
//    @Override
//    public void setEngine(ScorpioEngine scorpioEngine) {
//        this.engine = scorpioEngine;
//    }
//}


// ******* Deep Copy
/*
 * object ki copy mat karo new object create karo
 *
 */

public class Scorpio implements IScorpioPrototype {

    //default engine
    ScorpioEngine engine ;

    public Scorpio()
    {
        this.engine = new ScorpioEngine();
    }

    // this is private so class mein se hi call hoga
    private Scorpio(ScorpioEngine scorpioEngine)
    {
        this.engine = new ScorpioEngine(scorpioEngine);
    }

    @Override
    public void Start()
    {
        System.out.println("Start ho gayi");
    }

    @Override
    public IScorpioPrototype clone () {

//        IScorpioPrototype clonedScorpio = new Scorpio();
        // ab yaha hamesha Scorpio N ka engine banega
        // aur classic kiya then always classic
//        clonedScorpio.setEngine(new ScorpioNEngine());

        // ab ye nayi object return kar rahi hain na ki same
        // so yaha deep copy hain
        IScorpioPrototype clonedScorpio = new Scorpio(this.engine);
        return clonedScorpio;

    }

    @Override
    public void setEngine(ScorpioEngine scorpioEngine) {
        this.engine = scorpioEngine;
    }}

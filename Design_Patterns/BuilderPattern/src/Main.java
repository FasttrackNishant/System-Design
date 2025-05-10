import Models.ICar;

public class Main {
    public static void main(String[] args) {

        ICarBuilder builder = new ScorpioBuilder();
        Director director = new Director(builder);
        director.Construct(false);
        ICar car = builder.build();

    }

}
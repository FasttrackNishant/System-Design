import sourcevehiclebuilder.Director;
import sourcevehiclebuilder.ICarBuilder;
import sourcevehiclebuilder.Models.ICar;
import sourcevehiclebuilder.ScorpioBuilder;

public class MainSource {

    public static void main(String[] args) {
        ICarBuilder builder = new ScorpioBuilder();
        Director director = new Director(builder);
        director.Construct(false);
        ICar car = builder.build();
    }

};
import httpbuilder.HttpRequest;
import vehiclebuilder.Car;
import vehiclebuilder.Director;
import vehiclebuilder.FortunerBuilder;
import vehiclebuilder.ScorpioBuilder;

public class Main {
    public static void main(String[] args) {

        HttpRequest request1 = new HttpRequest.Builder("APi URL").build();

        HttpRequest request2 = new HttpRequest.Builder("New url").body("This is body").build();

        System.out.println(request2.getUrl() + " "+ request2.getBody());

        Director director = new Director();

        FortunerBuilder fortunerBuilder = new FortunerBuilder();
        director.setBuilder(fortunerBuilder);
        Car fortuner = director.constructCar();
        System.out.println(fortuner);

        ScorpioBuilder scorpioBuilder = new ScorpioBuilder();
        director.setBuilder(scorpioBuilder);
        Car scorpio = director.constructCar();
        System.out.println(scorpio);
    }
}
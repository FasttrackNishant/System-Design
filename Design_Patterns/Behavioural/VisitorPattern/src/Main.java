import java.util.Iterator;

public class Main {
    public static void main(String[] args) {
        System.out.println("Hello, World!");

        Fleet fleet = new Fleet();

        ICar scorpio = new Scorpio();

        fleet.addCar(scorpio);

        ICar alto = new Alto();

        fleet.addCar(alto);


        Iterator<ICar> cars = fleet.getIterator();
//        InsurancePriceVisitor insurancePriceVisitor = new InsurancePriceVisitor();
        ICarVisitor insurancePriceVisitor = new InsurancePriceVisitor();

        while (cars.hasNext()) {
            cars.next().accept(insurancePriceVisitor);
        }
    }
}
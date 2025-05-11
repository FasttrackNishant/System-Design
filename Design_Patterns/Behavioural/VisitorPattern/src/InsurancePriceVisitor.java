public class InsurancePriceVisitor implements ICarVisitor {

    // multiple visit functions specific to car type

    public void visitScorpio(Scorpio scorpio) {
        System.out.println("I am visiting scorpio");

    }

    public void visitAlto(Alto alto) {
        System.out.println("I am visiting Alto");
    }
}

public class Scorpio implements ICar {

    @Override
    public void accept(ICarVisitor carVisitor) {
        // Imp imp imp imp imp
        carVisitor.visitScorpio(this);

    }
}

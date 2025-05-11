public class Alto implements ICar {

    @Override
    public void accept(ICarVisitor carVisitor) {
        carVisitor.visitAlto(this);
    }
}

public interface ICar {

    // this is imp function from double dispatch pov
    public void accept(ICarVisitor carVisitor);

}

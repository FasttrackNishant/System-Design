package vehicle;

abstract class CarDecorator implements Car{

    protected final Car inner;

    CarDecorator(Car inner)
    {
        this.inner =inner;
    }

    @Override
    public void start() {
        inner.start();
    }

    @Override
    public void stop() {
        inner.stop();
    }

    @Override
    public int getWeight() {
        return inner.getWeight();
    }
}

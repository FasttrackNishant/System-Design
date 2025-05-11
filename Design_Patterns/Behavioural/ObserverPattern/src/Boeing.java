public class Boeing implements IAirCraft, IObserver {

    ISubject tower;

    public Boeing(ISubject tower) {
        this.tower = tower;
    }

    @Override
    public void fly() {
        tower.addObserver(this);
        System.out.println("Boeing is flying");
    }

    @Override
    public void land() {

        //land ke bad unsubscribe karto
        tower.removeObserver(this);
        System.out.println("Boss Land ho gaye hum");
    }

    @Override
    public void proceed(Object newState) {
        System.out.println("I am executing proceed method");
    }
}

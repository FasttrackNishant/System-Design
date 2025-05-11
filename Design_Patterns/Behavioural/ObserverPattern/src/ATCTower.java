import java.util.ArrayList;
import java.util.List;

public class ATCTower implements ISubject {

    List<IObserver> observers = new ArrayList<>();


    @Override
    public void addObserver(IObserver observer) {
        System.out.println("Adding Observer in Tower");
        observers.add(observer);
    }

    @Override
    public void removeObserver(IObserver observer) {
        System.out.println("Removing Observer in Tower");
        observers.remove(observer);
    }

    @Override
    public void notifyObserver() {

        //open ended function

        for (IObserver observer : observers) {
            observer.proceed(this);
        }

    }
}

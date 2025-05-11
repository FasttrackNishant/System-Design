import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

public class Fleet {

    List<ICar> cars = new ArrayList<>();

    public Iterator<ICar> getIterator() {
        return cars.iterator();
    }

    public void addCar(ICar car) {
        cars.add(car);
    }

}
